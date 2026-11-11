package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.MovementType;
import com.intellistock.model.Product;
import com.intellistock.model.Role;
import com.intellistock.model.StockMovement;
import com.intellistock.model.User;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.UiSupport;
import com.intellistock.util.Dates;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class StockMovementsPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final JComboBox<Product> productSelect = new JComboBox<>();
    private final JLabel currentStockLabel = new JLabel("Stock: —");
    private final JComboBox<MovementType> typeSelect = new JComboBox<>();
    private final JTextField quantityField = UiSupport.field();
    private final JTextField referenceField = UiSupport.field();
    private final JTextField reasonField = UiSupport.field();
    private final JButton submitMovementButton = Theme.primary("Record Movement");

    private final JComboBox<String> filterProductCombo = new JComboBox<>();
    private final JComboBox<String> filterTypeCombo = new JComboBox<>();
    private final JButton refreshButton = Theme.secondary("Refresh");

    private final DefaultTableModel tableModel = UiSupport.model(
            "ID", "When", "SKU", "Product", "Type", "Change", "Previous", "Balance", "User", "Reference", "Reason"
    );
    private final JTable table = UiSupport.table(tableModel);

    private List<Product> catalog = new ArrayList<>();
    private List<StockMovement> loadedMovements = new ArrayList<>();

    public StockMovementsPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Stock Movements"), BorderLayout.WEST);
        header.add(Theme.muted("Record receipts, dispatches, returns, damaged items, and audit-logged adjustments."),
                BorderLayout.SOUTH);

        // Record Movement Form Card
        JPanel formCard = new JPanel(new BorderLayout(10, 10));
        Theme.card(formCard);
        formCard.add(Theme.title("Record New Movement"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 8, 4, 8);
        c.weightx = 0.5;

        productSelect.setPreferredSize(new Dimension(280, 28));
        typeSelect.setPreferredSize(new Dimension(180, 28));
        currentStockLabel.setFont(Theme.HEADING);
        currentStockLabel.setForeground(Theme.INFO);

        productSelect.addActionListener(e -> updateStockDisplay());

        c.gridy = 0; c.gridx = 0;
        grid.add(UiSupport.labeled("Select Product *", productSelect, new JLabel(" ")), c);
        c.gridx = 1;
        JPanel stockBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        stockBox.setOpaque(false);
        stockBox.add(currentStockLabel);
        grid.add(stockBox, c);

        c.gridy = 1; c.gridx = 0;
        grid.add(UiSupport.labeled("Movement Type *", typeSelect, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Quantity *", quantityField, new JLabel(" ")), c);

        c.gridy = 2; c.gridx = 0;
        grid.add(UiSupport.labeled("Reference (e.g. PO#, Invoice#, Bill)", referenceField, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Reason / Notes (Required for Damage/Adjustment)", reasonField, new JLabel(" ")), c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(submitMovementButton);

        formCard.add(grid, BorderLayout.CENTER);
        formCard.add(actions, BorderLayout.SOUTH);

        // Movement History Table Card
        JPanel historyCard = new JPanel(new BorderLayout(8, 8));
        Theme.card(historyCard);

        JPanel historyToolbar = new JPanel(new BorderLayout());
        historyToolbar.setOpaque(false);
        JLabel histTitle = new JLabel("Stock Movement History");
        histTitle.setFont(Theme.HEADING);
        historyToolbar.add(histTitle, BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);
        filterProductCombo.setPreferredSize(new Dimension(160, 28));
        filterTypeCombo.setPreferredSize(new Dimension(140, 28));
        filterTypeCombo.addItem("All Movement Types");
        for (MovementType mt : MovementType.values()) {
            filterTypeCombo.addItem(mt.name());
        }

        filters.add(new JLabel("Product:"));
        filters.add(filterProductCombo);
        filters.add(new JLabel("Type:"));
        filters.add(filterTypeCombo);
        filters.add(refreshButton);
        historyToolbar.add(filters, BorderLayout.EAST);

        historyCard.add(historyToolbar, BorderLayout.NORTH);
        historyCard.add(UiSupport.scroll(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(formCard, BorderLayout.CENTER);

        root.add(top, BorderLayout.NORTH);
        root.add(historyCard, BorderLayout.CENTER);

        submitMovementButton.addActionListener(e -> submitMovement());
        refreshButton.addActionListener(e -> refresh());
        filterProductCombo.addActionListener(e -> applyFilter());
        filterTypeCombo.addActionListener(e -> applyFilter());
    }

    @Override
    public String id() {
        return "movements";
    }

    @Override
    public String title() {
        return "Stock Movements";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        User user = AppContext.get().auth().currentUser();
        boolean isAdmin = user != null && user.getRole() == Role.ADMIN;

        new SwingWorker<Void, Void>() {
            private List<Product> products;
            private List<StockMovement> movements;

            @Override
            protected Void doInBackground() {
                products = AppContext.get().products().list(false);
                movements = AppContext.get().inventory().all();
                return null;
            }

            @Override
            protected void done() {
                catalog = products != null ? products : List.of();
                loadedMovements = movements != null ? movements : List.of();

                // Repopulate product dropdown
                Product cur = (Product) productSelect.getSelectedItem();
                productSelect.removeAllItems();
                for (Product p : catalog) {
                    productSelect.addItem(p);
                }
                if (cur != null) {
                    for (int i = 0; i < productSelect.getItemCount(); i++) {
                        if (productSelect.getItemAt(i).getId() == cur.getId()) {
                            productSelect.setSelectedIndex(i);
                            break;
                        }
                    }
                }
                updateStockDisplay();

                // Repopulate types allowed for user
                typeSelect.removeAllItems();
                typeSelect.addItem(MovementType.RECEIPT);
                typeSelect.addItem(MovementType.DISPATCH);
                typeSelect.addItem(MovementType.RETURN);
                typeSelect.addItem(MovementType.DAMAGED);
                if (isAdmin) {
                    typeSelect.addItem(MovementType.ADJUSTMENT);
                    typeSelect.addItem(MovementType.CORRECTION);
                }

                // Repopulate filter product combo
                String curFilterProd = (String) filterProductCombo.getSelectedItem();
                filterProductCombo.removeAllItems();
                filterProductCombo.addItem("All Products");
                for (Product p : catalog) {
                    filterProductCombo.addItem(p.getSku());
                }
                if (curFilterProd != null) filterProductCombo.setSelectedItem(curFilterProd);

                applyFilter();
            }
        }.execute();
    }

    public void selectProduct(long productId) {
        for (int i = 0; i < productSelect.getItemCount(); i++) {
            Product p = productSelect.getItemAt(i);
            if (p != null && p.getId() == productId) {
                productSelect.setSelectedIndex(i);
                updateStockDisplay();
                break;
            }
        }
    }

    private void updateStockDisplay() {
        Product p = (Product) productSelect.getSelectedItem();
        if (p != null) {
            currentStockLabel.setText("Current Stock: " + p.getQuantity() + " " + p.getUnit());
        } else {
            currentStockLabel.setText("Stock: —");
        }
    }

    private void applyFilter() {
        String filterSku = (String) filterProductCombo.getSelectedItem();
        String filterType = (String) filterTypeCombo.getSelectedItem();

        tableModel.setRowCount(0);
        for (StockMovement m : loadedMovements) {
            if (filterSku != null && !"All Products".equals(filterSku)) {
                if (!filterSku.equalsIgnoreCase(m.getSku())) continue;
            }
            if (filterType != null && !"All Movement Types".equals(filterType)) {
                if (m.getMovementType() == null || !filterType.equalsIgnoreCase(m.getMovementType().name())) continue;
            }

            tableModel.addRow(new Object[]{
                    m.getId(),
                    Dates.formatDisplay(m.getOccurredAt()),
                    m.getSku() != null ? m.getSku() : "—",
                    m.getProductName() != null ? m.getProductName() : "—",
                    m.getMovementType(),
                    (m.getQuantityDelta() > 0 ? "+" : "") + m.getQuantityDelta(),
                    m.getPreviousQuantity(),
                    m.getResultingQuantity(),
                    m.getUsername() != null ? m.getUsername() : "—",
                    m.getReference() != null ? m.getReference() : "—",
                    m.getReason() != null ? m.getReason() : "—"
            });
        }
    }

    private void submitMovement() {
        Product p = (Product) productSelect.getSelectedItem();
        if (p == null) {
            UiSupport.error(root, "Please select a product.");
            return;
        }

        MovementType type = (MovementType) typeSelect.getSelectedItem();
        if (type == null) {
            UiSupport.error(root, "Please select a movement type.");
            return;
        }

        String qtyStr = quantityField.getText().trim();
        if (qtyStr.isEmpty()) {
            UiSupport.error(root, "Quantity is required.");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
            if (type == MovementType.ADJUSTMENT || type == MovementType.CORRECTION) {
                if (qty == 0) throw new IllegalArgumentException("Adjustment quantity cannot be zero.");
            } else {
                if (qty <= 0) throw new IllegalArgumentException("Quantity must be a positive integer greater than zero.");
            }
        } catch (NumberFormatException e) {
            UiSupport.error(root, "Quantity must be a valid whole number"
                    + (type == MovementType.ADJUSTMENT || type == MovementType.CORRECTION ? " (e.g. -5 to deduct, 5 to add)." : "."));
            return;
        } catch (IllegalArgumentException e) {
            UiSupport.error(root, e.getMessage());
            return;
        }

        String ref = referenceField.getText().trim();
        String reason = reasonField.getText().trim();

        if ((type == MovementType.DAMAGED || type == MovementType.ADJUSTMENT || type == MovementType.CORRECTION)
                && reason.isEmpty()) {
            UiSupport.error(root, "A reason is mandatory for " + type + " movements.");
            return;
        }

        User user = AppContext.get().auth().currentUser();
        submitMovementButton.setEnabled(false);

        new SwingWorker<StockMovement, Void>() {
            @Override
            protected StockMovement doInBackground() {
                return switch (type) {
                    case RECEIPT -> AppContext.get().inventory().receive(user, p.getId(), qty, reason, ref);
                    case DISPATCH -> AppContext.get().inventory().dispatch(user, p.getId(), qty, reason, ref);
                    case RETURN -> AppContext.get().inventory().recordReturn(user, p.getId(), qty, reason, ref);
                    case DAMAGED -> AppContext.get().inventory().damaged(user, p.getId(), qty, reason);
                    case ADJUSTMENT -> AppContext.get().inventory().adjust(user, p.getId(), qty, reason);
                    case CORRECTION -> AppContext.get().inventory().correct(user, p.getId(), qty, reason);
                    default -> throw new IllegalArgumentException("Unsupported movement type");
                };
            }

            @Override
            protected void done() {
                submitMovementButton.setEnabled(true);
                try {
                    StockMovement sm = get();
                    UiSupport.info(root, "Stock movement recorded successfully!\nProduct: " + p.getName()
                            + "\nType: " + sm.getMovementType()
                            + "\nChange: " + (sm.getQuantityDelta() > 0 ? "+" : "") + sm.getQuantityDelta()
                            + "\nNew Stock Balance: " + sm.getResultingQuantity() + " " + p.getUnit());
                    quantityField.setText("");
                    referenceField.setText("");
                    reasonField.setText("");
                    refresh();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    UiSupport.error(root, "Movement failed: " + cause.getMessage());
                }
            }
        }.execute();
    }
}
