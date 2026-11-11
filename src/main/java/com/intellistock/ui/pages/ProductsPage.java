package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.Category;
import com.intellistock.model.Product;
import com.intellistock.model.StockStatus;
import com.intellistock.model.Supplier;
import com.intellistock.model.User;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.StatusBadge;
import com.intellistock.ui.components.UiSupport;
import com.intellistock.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
import java.util.ArrayList;
import java.util.List;

public class ProductsPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final JTextField searchField = UiSupport.field();
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JCheckBox includeArchivedCheck = new JCheckBox("Include Archived");

    private final DefaultTableModel tableModel = UiSupport.model(
            "ID", "SKU", "Name", "Category", "Quantity", "Unit", "Cost", "Selling", "Status", "Location", "Rack", "Expiry", "Supplier"
    );
    private final JTable table = UiSupport.table(tableModel);

    private final JButton registerButton = Theme.primary("+ Register Product");
    private final JButton editButton = Theme.secondary("Edit Selected");
    private final JButton archiveButton = Theme.secondary("Archive Selected");
    private final JButton movementButton = Theme.secondary("Stock Movement");
    private final JButton refreshButton = Theme.secondary("Refresh");

    private List<Product> loadedProducts = new ArrayList<>();

    public ProductsPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Product Inventory"), BorderLayout.WEST);
        header.add(Theme.muted("View, search, filter, and manage catalog items with real-time SQLite stock tracking."),
                BorderLayout.SOUTH);

        // Filter / Search Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(10, 10));
        toolbar.setOpaque(false);

        JPanel filtersLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtersLeft.setOpaque(false);
        searchField.setPreferredSize(new Dimension(180, 28));
        filtersLeft.add(new JLabel("Search:"));
        filtersLeft.add(searchField);

        categoryFilter.setPreferredSize(new Dimension(140, 28));
        filtersLeft.add(new JLabel("Category:"));
        filtersLeft.add(categoryFilter);

        statusFilter.setPreferredSize(new Dimension(120, 28));
        statusFilter.addItem("All Statuses");
        statusFilter.addItem("NORMAL");
        statusFilter.addItem("LOW");
        statusFilter.addItem("CRITICAL");
        statusFilter.addItem("OUT_OF_STOCK");
        statusFilter.addItem("ARCHIVED");
        filtersLeft.add(new JLabel("Status:"));
        filtersLeft.add(statusFilter);

        includeArchivedCheck.setOpaque(false);
        filtersLeft.add(includeArchivedCheck);

        JButton searchBtn = Theme.secondary("Filter");
        searchBtn.addActionListener(e -> applyFilter());
        filtersLeft.add(searchBtn);

        JPanel actionsRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actionsRight.setOpaque(false);
        actionsRight.add(movementButton);
        actionsRight.add(editButton);
        actionsRight.add(archiveButton);
        actionsRight.add(refreshButton);
        actionsRight.add(registerButton);

        toolbar.add(filtersLeft, BorderLayout.CENTER);
        toolbar.add(actionsRight, BorderLayout.EAST);

        // Table Panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        Theme.card(tablePanel);

        // Customize status renderer
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                if (value instanceof StockStatus ss) {
                    JLabel badge = StatusBadge.of(ss);
                    badge.setOpaque(true);
                    return badge;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        tablePanel.add(UiSupport.scroll(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(tablePanel, BorderLayout.CENTER);

        // Listeners
        registerButton.addActionListener(e -> frame.showPage("register"));
        refreshButton.addActionListener(e -> refresh());
        editButton.addActionListener(e -> editSelected());
        archiveButton.addActionListener(e -> archiveSelected());
        movementButton.addActionListener(e -> recordMovementForSelected());
        includeArchivedCheck.addActionListener(e -> refresh());
        categoryFilter.addActionListener(e -> applyFilter());
        statusFilter.addActionListener(e -> applyFilter());
        searchField.addActionListener(e -> applyFilter());
    }

    @Override
    public String id() {
        return "products";
    }

    @Override
    public String title() {
        return "Products";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        boolean incArchived = includeArchivedCheck.isSelected();
        new SwingWorker<List<Product>, Void>() {
            private List<Category> categories;

            @Override
            protected List<Product> doInBackground() {
                categories = AppContext.get().products().categories();
                return AppContext.get().products().list(incArchived);
            }

            @Override
            protected void done() {
                try {
                    loadedProducts = get();
                    // Update categories dropdown while preserving selection
                    String curCat = (String) categoryFilter.getSelectedItem();
                    categoryFilter.removeAllItems();
                    categoryFilter.addItem("All Categories");
                    if (categories != null) {
                        for (Category c : categories) {
                            categoryFilter.addItem(c.getName());
                        }
                    }
                    if (curCat != null) {
                        categoryFilter.setSelectedItem(curCat);
                    }
                    applyFilter();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String query = searchField.getText().trim().toLowerCase();
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        String selectedStatus = (String) statusFilter.getSelectedItem();

        tableModel.setRowCount(0);
        for (Product p : loadedProducts) {
            if (!query.isEmpty()) {
                boolean matchSku = p.getSku() != null && p.getSku().toLowerCase().contains(query);
                boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(query);
                if (!matchSku && !matchName) continue;
            }

            if (selectedCategory != null && !"All Categories".equals(selectedCategory)) {
                if (!selectedCategory.equalsIgnoreCase(p.getCategoryName())) continue;
            }

            if (selectedStatus != null && !"All Statuses".equals(selectedStatus)) {
                if (!p.getStockStatus().name().equalsIgnoreCase(selectedStatus)) continue;
            }

            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getSku(),
                    p.getName(),
                    p.getCategoryName() != null ? p.getCategoryName() : "—",
                    p.getQuantity(),
                    p.getUnit(),
                    Money.format(p.getCostPriceCents()),
                    Money.format(p.getSellingPriceCents()),
                    p.getStockStatus(),
                    p.getWarehouseLocation() != null ? p.getWarehouseLocation() : "—",
                    p.getRackShelf() != null ? p.getRackShelf() : "—",
                    p.getExpiryDate() != null ? p.getExpiryDate() : "—",
                    p.getPreferredSupplierName() != null ? p.getPreferredSupplierName() : "—"
            });
        }
    }

    private Product getSelectedProduct() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            UiSupport.error(root, "Please select a product from the table first.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        long id = (Long) tableModel.getValueAt(modelRow, 0);
        for (Product p : loadedProducts) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    private void archiveSelected() {
        Product selected = getSelectedProduct();
        if (selected == null) return;

        if (selected.isArchived()) {
            UiSupport.info(root, "This product is already archived.");
            return;
        }

        if (!UiSupport.confirm(root, "Are you sure you want to archive product '" + selected.getSku() + " — " + selected.getName() + "'?\nIt will be hidden from regular inventory views.")) {
            return;
        }

        User user = AppContext.get().auth().currentUser();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                AppContext.get().products().archive(user, selected.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UiSupport.info(root, "Product archived successfully.");
                    refresh();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    UiSupport.error(root, "Failed to archive product: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void recordMovementForSelected() {
        Product selected = getSelectedProduct();
        if (selected == null) return;
        frame.showMovementsForProduct(selected.getId());
    }

    private void editSelected() {
        Product selected = getSelectedProduct();
        if (selected == null) return;

        JDialog dialog = new JDialog(frame, "Edit Product: " + selected.getSku(), true);
        dialog.setSize(640, 520);
        dialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        Theme.card(form);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 8, 6, 8);
        c.weightx = 0.5;

        JTextField nameFld = UiSupport.field();
        nameFld.setText(selected.getName());

        JTextField unitFld = UiSupport.field();
        unitFld.setText(selected.getUnit());

        JTextField reorderFld = UiSupport.field();
        reorderFld.setText(String.valueOf(selected.getReorderThreshold()));

        JTextField criticalFld = UiSupport.field();
        criticalFld.setText(String.valueOf(selected.getCriticalThreshold()));

        JTextField costFld = UiSupport.field();
        costFld.setText(Money.toPlain(selected.getCostPriceCents()));

        JTextField sellFld = UiSupport.field();
        sellFld.setText(Money.toPlain(selected.getSellingPriceCents()));

        JTextField whFld = UiSupport.field();
        whFld.setText(selected.getWarehouseLocation() != null ? selected.getWarehouseLocation() : "");

        JTextField rackFld = UiSupport.field();
        rackFld.setText(selected.getRackShelf() != null ? selected.getRackShelf() : "");

        JTextField expiryFld = UiSupport.field();
        expiryFld.setText(selected.getExpiryDate() != null ? selected.getExpiryDate() : "");

        JTextField barcodeFld = UiSupport.field();
        barcodeFld.setText(selected.getBarcode() != null ? selected.getBarcode() : "");

        // Category combo
        JComboBox<Category> catCombo = new JComboBox<>();
        List<Category> allCats = AppContext.get().products().categories();
        Category currentCat = null;
        for (Category cat : allCats) {
            catCombo.addItem(cat);
            if (cat.getId() == selected.getCategoryId()) currentCat = cat;
        }
        if (currentCat != null) catCombo.setSelectedItem(currentCat);

        // Supplier combo
        JComboBox<SupplierOption> supCombo = new JComboBox<>();
        supCombo.addItem(new SupplierOption(null, "-- None --"));
        List<Supplier> allSups = AppContext.get().suppliers().list(false);
        SupplierOption currentSup = null;
        for (Supplier s : allSups) {
            SupplierOption opt = new SupplierOption(s.getId(), s.getCompanyName());
            supCombo.addItem(opt);
            if (selected.getPreferredSupplierId() != null && selected.getPreferredSupplierId() == s.getId()) {
                currentSup = opt;
            }
        }
        if (currentSup != null) supCombo.setSelectedItem(currentSup);

        c.gridy = 0; c.gridx = 0;
        form.add(UiSupport.labeled("Product Name *", nameFld, new JLabel(" ")), c);
        c.gridx = 1;
        form.add(UiSupport.labeled("Category *", catCombo, new JLabel(" ")), c);

        c.gridy = 1; c.gridx = 0;
        form.add(UiSupport.labeled("Unit *", unitFld, new JLabel(" ")), c);
        c.gridx = 1;
        form.add(UiSupport.labeled("Barcode", barcodeFld, new JLabel(" ")), c);

        c.gridy = 2; c.gridx = 0;
        form.add(UiSupport.labeled("Reorder Threshold *", reorderFld, new JLabel(" ")), c);
        c.gridx = 1;
        form.add(UiSupport.labeled("Critical Threshold *", criticalFld, new JLabel(" ")), c);

        c.gridy = 3; c.gridx = 0;
        form.add(UiSupport.labeled("Cost Price ($) *", costFld, new JLabel(" ")), c);
        c.gridx = 1;
        form.add(UiSupport.labeled("Selling Price ($) *", sellFld, new JLabel(" ")), c);

        c.gridy = 4; c.gridx = 0;
        form.add(UiSupport.labeled("Warehouse Location", whFld, new JLabel(" ")), c);
        c.gridx = 1;
        form.add(UiSupport.labeled("Rack / Shelf", rackFld, new JLabel(" ")), c);

        c.gridy = 5; c.gridx = 0;
        form.add(UiSupport.labeled("Expiry Date (YYYY-MM-DD)", expiryFld, new JLabel(" ")), c);
        c.gridx = 1;
        form.add(UiSupport.labeled("Preferred Supplier", supCombo, new JLabel(" ")), c);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.setOpaque(false);
        JButton cancelBtn = Theme.secondary("Cancel");
        JButton saveBtn = Theme.primary("Save Changes");

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                String name = nameFld.getText().trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Product name cannot be empty.");
                Category selectedC = (Category) catCombo.getSelectedItem();
                if (selectedC == null) throw new IllegalArgumentException("Category is required.");

                selected.setName(name);
                selected.setCategoryId(selectedC.getId());
                selected.setUnit(unitFld.getText().trim());
                selected.setBarcode(barcodeFld.getText().trim().isEmpty() ? null : barcodeFld.getText().trim());
                selected.setReorderThreshold(Integer.parseInt(reorderFld.getText().trim()));
                selected.setCriticalThreshold(Integer.parseInt(criticalFld.getText().trim()));
                selected.setCostPriceCents(Money.toCents(costFld.getText().trim()));
                selected.setSellingPriceCents(Money.toCents(sellFld.getText().trim()));
                selected.setWarehouseLocation(whFld.getText().trim().isEmpty() ? null : whFld.getText().trim());
                selected.setRackShelf(rackFld.getText().trim().isEmpty() ? null : rackFld.getText().trim());
                selected.setExpiryDate(expiryFld.getText().trim().isEmpty() ? null : expiryFld.getText().trim());

                SupplierOption so = (SupplierOption) supCombo.getSelectedItem();
                selected.setPreferredSupplierId(so != null ? so.id : null);

                User user = AppContext.get().auth().currentUser();
                AppContext.get().products().updateMetadata(user, selected);
                UiSupport.info(dialog, "Product updated successfully.");
                dialog.dispose();
                refresh();
            } catch (Exception ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                UiSupport.error(dialog, "Error: " + cause.getMessage());
            }
        });

        buttonBar.add(cancelBtn);
        buttonBar.add(saveBtn);

        panel.add(form, BorderLayout.CENTER);
        panel.add(buttonBar, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private static class SupplierOption {
        final Long id;
        final String name;

        SupplierOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
