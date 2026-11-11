package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.OrderStatus;
import com.intellistock.model.Product;
import com.intellistock.model.PurchaseOrder;
import com.intellistock.model.PurchaseOrderItem;
import com.intellistock.model.Supplier;
import com.intellistock.model.User;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.StatusBadge;
import com.intellistock.ui.components.UiSupport;
import com.intellistock.util.Dates;
import com.intellistock.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PurchaseOrdersPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JButton newOrderButton = Theme.primary("+ New Order");
    private final JButton submitOrderButton = Theme.secondary("Submit (Draft -> Pending)");
    private final JButton receiveButton = Theme.secondary("Receive Goods");
    private final JButton cancelButton = Theme.secondary("Cancel Order");
    private final JButton refreshButton = Theme.secondary("Refresh");

    private final DefaultTableModel orderModel = UiSupport.model(
            "PO #", "Supplier", "Status", "Expected Date", "Total Cost", "Created By", "Created At"
    );
    private final JTable orderTable = UiSupport.table(orderModel);

    private final DefaultTableModel itemModel = UiSupport.model(
            "SKU", "Product Name", "Ordered", "Received", "Outstanding", "Unit Cost", "Line Total"
    );
    private final JTable itemTable = UiSupport.table(itemModel);
    private final JLabel itemsTitle = new JLabel("Order Items");

    private List<PurchaseOrder> loadedOrders = new ArrayList<>();

    public PurchaseOrdersPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Purchase Orders"), BorderLayout.WEST);
        header.add(Theme.muted("Procurement workflows: draft creation, submission, and transaction-safe partial/full goods receipts."),
                BorderLayout.SOUTH);

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        left.setOpaque(false);
        statusFilter.setPreferredSize(new Dimension(160, 28));
        statusFilter.addItem("All Statuses");
        for (OrderStatus st : OrderStatus.values()) {
            statusFilter.addItem(st.name());
        }
        left.add(new JLabel("Status:"));
        left.add(statusFilter);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        right.add(submitOrderButton);
        right.add(receiveButton);
        right.add(cancelButton);
        right.add(refreshButton);
        right.add(newOrderButton);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        // Top table: Orders
        JPanel orderCard = new JPanel(new BorderLayout());
        Theme.card(orderCard);

        orderTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                if (value instanceof OrderStatus os) {
                    JLabel b = StatusBadge.of(os);
                    b.setOpaque(true);
                    return b;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        orderCard.add(UiSupport.scroll(orderTable), BorderLayout.CENTER);

        // Bottom table: Items
        JPanel itemCard = new JPanel(new BorderLayout(6, 6));
        Theme.card(itemCard);
        itemsTitle.setFont(Theme.HEADING);
        itemCard.add(itemsTitle, BorderLayout.NORTH);
        itemCard.add(UiSupport.scroll(itemTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, orderCard, itemCard);
        splitPane.setResizeWeight(0.55);
        splitPane.setOpaque(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);

        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedOrderItems();
            }
        });

        statusFilter.addActionListener(e -> applyFilter());
        refreshButton.addActionListener(e -> refresh());
        newOrderButton.addActionListener(e -> openNewOrderDialog());
        submitOrderButton.addActionListener(e -> submitSelectedOrder());
        receiveButton.addActionListener(e -> receiveSelectedOrder());
        cancelButton.addActionListener(e -> cancelSelectedOrder());
    }

    @Override
    public String id() {
        return "orders";
    }

    @Override
    public String title() {
        return "Purchase Orders";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        new SwingWorker<List<PurchaseOrder>, Void>() {
            @Override
            protected List<PurchaseOrder> doInBackground() {
                return AppContext.get().purchaseOrders().list();
            }

            @Override
            protected void done() {
                try {
                    loadedOrders = get();
                    applyFilter();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String filter = (String) statusFilter.getSelectedItem();
        orderModel.setRowCount(0);
        for (PurchaseOrder po : loadedOrders) {
            if (filter != null && !"All Statuses".equals(filter)) {
                if (po.getStatus() == null || !filter.equalsIgnoreCase(po.getStatus().name())) continue;
            }

            orderModel.addRow(new Object[]{
                    po.getId(),
                    po.getSupplierName() != null ? po.getSupplierName() : "Supplier #" + po.getSupplierId(),
                    po.getStatus(),
                    po.getExpectedDeliveryDate() != null ? po.getExpectedDeliveryDate() : "—",
                    Money.format(po.totalCents()),
                    po.getCreatedByName() != null ? po.getCreatedByName() : "User #" + po.getCreatedBy(),
                    Dates.formatDisplay(po.getCreatedAt())
            });
        }
        itemModel.setRowCount(0);
        itemsTitle.setText("Order Items");
    }

    private PurchaseOrder getSelectedOrder() {
        int row = orderTable.getSelectedRow();
        if (row < 0) {
            UiSupport.error(root, "Please select a purchase order from the table.");
            return null;
        }
        int modelRow = orderTable.convertRowIndexToModel(row);
        long id = (Long) orderModel.getValueAt(modelRow, 0);
        for (PurchaseOrder po : loadedOrders) {
            if (po.getId() == id) return po;
        }
        return null;
    }

    private void loadSelectedOrderItems() {
        PurchaseOrder po = getSelectedOrder();
        if (po == null) return;

        itemsTitle.setText("Order #" + po.getId() + " Items (" + po.getSupplierName() + " — Status: " + po.getStatus() + ")");
        itemModel.setRowCount(0);
        if (po.getItems().isEmpty()) {
            itemModel.addRow(new Object[]{"—", "No items recorded in this order", "", "", "", "", ""});
        } else {
            for (PurchaseOrderItem item : po.getItems()) {
                itemModel.addRow(new Object[]{
                        item.getSku() != null ? item.getSku() : "—",
                        item.getProductName() != null ? item.getProductName() : "Product #" + item.getProductId(),
                        item.getQuantityOrdered(),
                        item.getQuantityReceived(),
                        item.outstanding(),
                        Money.format(item.getUnitCostCents()),
                        Money.format(item.lineTotalCents())
                });
            }
        }
    }

    private void submitSelectedOrder() {
        PurchaseOrder po = getSelectedOrder();
        if (po == null) return;

        if (po.getStatus() != OrderStatus.DRAFT) {
            UiSupport.error(root, "Only DRAFT orders can be submitted to PENDING status.");
            return;
        }

        if (!UiSupport.confirm(root, "Submit Purchase Order #" + po.getId() + " to supplier?\nThis sets status to PENDING.")) {
            return;
        }

        User user = AppContext.get().auth().currentUser();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                AppContext.get().purchaseOrders().submit(user, po.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UiSupport.info(root, "Purchase Order #" + po.getId() + " submitted successfully.");
                    refresh();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    UiSupport.error(root, "Failed: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void cancelSelectedOrder() {
        PurchaseOrder po = getSelectedOrder();
        if (po == null) return;

        if (po.getStatus() == OrderStatus.RECEIVED || po.getStatus() == OrderStatus.CANCELLED) {
            UiSupport.error(root, "Cannot cancel an order that is already " + po.getStatus() + ".");
            return;
        }

        if (!UiSupport.confirm(root, "Are you sure you want to cancel Purchase Order #" + po.getId() + "?")) {
            return;
        }

        User user = AppContext.get().auth().currentUser();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                AppContext.get().purchaseOrders().cancel(user, po.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UiSupport.info(root, "Purchase order cancelled.");
                    refresh();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    UiSupport.error(root, "Cancellation failed: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void receiveSelectedOrder() {
        PurchaseOrder po = getSelectedOrder();
        if (po == null) return;

        if (po.getStatus() != OrderStatus.PENDING && po.getStatus() != OrderStatus.PARTIALLY_RECEIVED) {
            UiSupport.error(root, "Goods receipt can only be processed for PENDING or PARTIALLY_RECEIVED orders.\nCurrent status: " + po.getStatus());
            return;
        }

        JDialog dialog = new JDialog(frame, "Goods Receipt — Order #" + po.getId(), true);
        dialog.setSize(620, 420);
        dialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel info = new JLabel("<html><b>Supplier:</b> " + po.getSupplierName()
                + "<br>Enter quantity received for each line item below. Received stock will be automatically added to inventory with an audit trail.</html>");
        info.setFont(Theme.BODY);
        panel.add(info, BorderLayout.NORTH);

        DefaultTableModel receiptModel = new DefaultTableModel(new String[]{"Product ID", "SKU", "Product", "Ordered", "Received", "Outstanding", "Receive Now"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // only "Receive Now" is editable
            }
        };

        for (PurchaseOrderItem item : po.getItems()) {
            receiptModel.addRow(new Object[]{
                    item.getProductId(),
                    item.getSku(),
                    item.getProductName(),
                    item.getQuantityOrdered(),
                    item.getQuantityReceived(),
                    item.outstanding(),
                    String.valueOf(item.outstanding()) // prefill with full outstanding
            });
        }

        JTable receiptTable = new JTable(receiptModel);
        receiptTable.setRowHeight(26);
        panel.add(UiSupport.scroll(receiptTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        JButton cancelBtn = Theme.secondary("Cancel");
        JButton confirmBtn = Theme.primary("Confirm Goods Receipt");

        cancelBtn.addActionListener(e -> dialog.dispose());
        confirmBtn.addActionListener(e -> {
            try {
                if (receiptTable.isEditing()) {
                    receiptTable.getCellEditor().stopCellEditing();
                }
                Map<Long, Integer> receivedQuantities = new HashMap<>();
                for (int r = 0; r < receiptModel.getRowCount(); r++) {
                    long prodId = (Long) receiptModel.getValueAt(r, 0);
                    int outstanding = (Integer) receiptModel.getValueAt(r, 5);
                    String receiveStr = receiptModel.getValueAt(r, 6).toString().trim();
                    int qty = receiveStr.isEmpty() ? 0 : Integer.parseInt(receiveStr);
                    if (qty < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
                    if (qty > outstanding) throw new IllegalArgumentException("Cannot receive more than outstanding quantity (" + outstanding + ") for line " + (r + 1));
                    if (qty > 0) {
                        receivedQuantities.put(prodId, qty);
                    }
                }

                if (receivedQuantities.isEmpty()) {
                    throw new IllegalArgumentException("Please enter a quantity greater than zero for at least one item.");
                }

                User user = AppContext.get().auth().currentUser();
                AppContext.get().purchaseOrders().receive(user, po.getId(), receivedQuantities, null);
                UiSupport.info(dialog, "Goods received successfully! Stock levels updated.");
                dialog.dispose();
                refresh();
            } catch (Exception ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                UiSupport.error(dialog, "Receipt failed: " + cause.getMessage());
            }
        });

        bottom.add(cancelBtn);
        bottom.add(confirmBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void openNewOrderDialog() {
        JDialog dialog = new JDialog(frame, "Create New Purchase Order", true);
        dialog.setSize(740, 540);
        dialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel topFields = new JPanel(new GridBagLayout());
        Theme.card(topFields);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 6, 4, 6);
        c.weightx = 0.5;

        JComboBox<Supplier> supplierCombo = new JComboBox<>();
        List<Supplier> sups = AppContext.get().suppliers().list(true);
        for (Supplier s : sups) supplierCombo.addItem(s);

        JTextField expectedField = UiSupport.field();
        expectedField.setText(LocalDate.now().plusDays(7).toString());

        JTextArea notesArea = new JTextArea(2, 20);
        JScrollPane notesScroll = new JScrollPane(notesArea);

        c.gridy = 0; c.gridx = 0;
        topFields.add(UiSupport.labeled("Supplier *", supplierCombo, new JLabel(" ")), c);
        c.gridx = 1;
        topFields.add(UiSupport.labeled("Expected Delivery Date (YYYY-MM-DD)", expectedField, new JLabel(" ")), c);

        c.gridy = 1; c.gridx = 0; c.gridwidth = 2;
        topFields.add(UiSupport.labeled("Order Notes / Instructions", notesScroll, new JLabel(" ")), c);

        // Lines table
        DefaultTableModel lineModel = new DefaultTableModel(new String[]{"Product", "Quantity", "Unit Cost ($)", "Line Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable lineTable = new JTable(lineModel);
        List<PurchaseOrderItem> orderItems = new ArrayList<>();

        JPanel linesPanel = new JPanel(new BorderLayout(6, 6));
        linesPanel.setOpaque(false);
        linesPanel.setBorder(BorderFactory.createTitledBorder("Order Line Items"));

        JPanel lineButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        lineButtons.setOpaque(false);
        JButton addLineBtn = Theme.primary("+ Add Line Item");
        JButton removeLineBtn = Theme.secondary("Remove Line");
        lineButtons.add(addLineBtn);
        lineButtons.add(removeLineBtn);

        linesPanel.add(lineButtons, BorderLayout.NORTH);
        linesPanel.add(UiSupport.scroll(lineTable), BorderLayout.CENTER);

        List<Product> allProducts = AppContext.get().products().list(false);

        addLineBtn.addActionListener(e -> {
            JDialog itemDialog = new JDialog(dialog, "Add Product Line", true);
            itemDialog.setSize(380, 260);
            itemDialog.setLocationRelativeTo(dialog);

            JPanel itemForm = new JPanel(new GridBagLayout());
            Theme.card(itemForm);
            GridBagConstraints ic = new GridBagConstraints();
            ic.fill = GridBagConstraints.HORIZONTAL;
            ic.insets = new Insets(6, 6, 6, 6);
            ic.weightx = 1.0;

            JComboBox<Product> prodCombo = new JComboBox<>();
            for (Product p : allProducts) prodCombo.addItem(p);

            JTextField qtyFld = UiSupport.field();
            qtyFld.setText("10");

            JTextField costFld = UiSupport.field();
            if (prodCombo.getItemCount() > 0) {
                costFld.setText(Money.toPlain(prodCombo.getItemAt(0).getCostPriceCents()));
            }

            prodCombo.addActionListener(ev -> {
                Product p = (Product) prodCombo.getSelectedItem();
                if (p != null) costFld.setText(Money.toPlain(p.getCostPriceCents()));
            });

            ic.gridy = 0; ic.gridx = 0;
            itemForm.add(UiSupport.labeled("Product *", prodCombo, new JLabel(" ")), ic);
            ic.gridy = 1;
            itemForm.add(UiSupport.labeled("Quantity Ordered *", qtyFld, new JLabel(" ")), ic);
            ic.gridy = 2;
            itemForm.add(UiSupport.labeled("Unit Cost ($) *", costFld, new JLabel(" ")), ic);

            JPanel ib = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            ib.setOpaque(false);
            JButton cancelItem = Theme.secondary("Cancel");
            JButton saveItem = Theme.primary("Add");
            cancelItem.addActionListener(ev -> itemDialog.dispose());
            saveItem.addActionListener(ev -> {
                try {
                    Product p = (Product) prodCombo.getSelectedItem();
                    if (p == null) throw new IllegalArgumentException("Product is required");
                    boolean alreadyPresent = orderItems.stream().anyMatch(item -> item.getProductId() == p.getId());
                    if (alreadyPresent) {
                        throw new IllegalArgumentException("Product '" + p.getSku() + "' is already in this purchase order. Please remove the existing line first if you wish to change it.");
                    }
                    int q = Integer.parseInt(qtyFld.getText().trim());
                    if (q <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
                    long cost = Money.toCents(costFld.getText().trim());

                    PurchaseOrderItem poi = new PurchaseOrderItem();
                    poi.setProductId(p.getId());
                    poi.setProductName(p.getName());
                    poi.setSku(p.getSku());
                    poi.setQuantityOrdered(q);
                    poi.setUnitCostCents(cost);

                    orderItems.add(poi);
                    lineModel.addRow(new Object[]{
                            p.getSku() + " — " + p.getName(),
                            q,
                            Money.format(cost),
                            Money.format(poi.lineTotalCents())
                    });
                    itemDialog.dispose();
                } catch (Exception ex) {
                    UiSupport.error(itemDialog, ex.getMessage());
                }
            });
            ib.add(cancelItem);
            ib.add(saveItem);

            JPanel iroot = new JPanel(new BorderLayout(8, 8));
            iroot.setBackground(Theme.BACKGROUND);
            iroot.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            iroot.add(itemForm, BorderLayout.CENTER);
            iroot.add(ib, BorderLayout.SOUTH);
            itemDialog.setContentPane(iroot);
            itemDialog.setVisible(true);
        });

        removeLineBtn.addActionListener(e -> {
            int row = lineTable.getSelectedRow();
            if (row >= 0 && row < orderItems.size()) {
                orderItems.remove(row);
                lineModel.removeRow(row);
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        JButton cancelDialogBtn = Theme.secondary("Cancel");
        JButton saveDraftBtn = Theme.primary("Save Draft Order");

        cancelDialogBtn.addActionListener(e -> dialog.dispose());
        saveDraftBtn.addActionListener(e -> {
            try {
                Supplier sup = (Supplier) supplierCombo.getSelectedItem();
                if (sup == null) throw new IllegalArgumentException("Supplier is required.");
                if (orderItems.isEmpty()) throw new IllegalArgumentException("Please add at least one line item.");

                PurchaseOrder newPo = new PurchaseOrder();
                newPo.setSupplierId(sup.getId());
                newPo.setExpectedDeliveryDate(expectedField.getText().trim().isEmpty() ? null : expectedField.getText().trim());
                newPo.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                newPo.setItems(orderItems);

                User user = AppContext.get().auth().currentUser();
                AppContext.get().purchaseOrders().saveDraft(user, newPo);
                UiSupport.info(dialog, "Purchase order draft created successfully!");
                dialog.dispose();
                refresh();
            } catch (Exception ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                UiSupport.error(dialog, "Error: " + cause.getMessage());
            }
        });

        bottom.add(cancelDialogBtn);
        bottom.add(saveDraftBtn);

        panel.add(topFields, BorderLayout.NORTH);
        panel.add(linesPanel, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
}
