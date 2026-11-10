package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.Category;
import com.intellistock.model.Product;
import com.intellistock.model.ProductRegistrationRequest;
import com.intellistock.model.Supplier;
import com.intellistock.model.User;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.UiSupport;
import com.intellistock.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class RegisterProductPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final JTextField nameField = UiSupport.field();
    private final JTextField skuField = UiSupport.field();
    private final JComboBox<CategoryItem> categoryCombo = new JComboBox<>();
    private final JTextField newCategoryField = UiSupport.field();
    private final JTextArea descriptionArea = new JTextArea(3, 20);
    private final JComboBox<String> unitCombo = new JComboBox<>(new String[]{
            "pieces", "boxes", "reams", "bags", "bottles", "cans", "kg", "meters", "liters", "packs"
    });
    private final JTextField initialQtyField = UiSupport.field();
    private final JTextField reorderField = UiSupport.field();
    private final JTextField criticalField = UiSupport.field();
    private final JTextField warehouseField = UiSupport.field();
    private final JTextField rackField = UiSupport.field();
    private final JComboBox<SupplierItem> supplierCombo = new JComboBox<>();
    private final JTextField costPriceField = UiSupport.field();
    private final JTextField sellingPriceField = UiSupport.field();
    private final JTextField barcodeField = UiSupport.field();
    private final JTextField expiryField = UiSupport.field();

    private final JLabel statusMessage = new JLabel(" ");
    private final JButton submitButton = Theme.primary("Register Product");
    private final JButton clearButton = Theme.secondary("Clear Form");
    private final JButton viewCatalogButton = Theme.secondary("View Products");

    public RegisterProductPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Register Product"), BorderLayout.WEST);
        header.add(Theme.muted("Add a new product to inventory. Required fields insert into SQLite via JDBC transactions."),
                BorderLayout.SOUTH);

        JPanel formCard = new JPanel(new BorderLayout(12, 12));
        Theme.card(formCard);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 10, 6, 10);
        c.weightx = 0.5;

        unitCombo.setEditable(true);
        unitCombo.setPreferredSize(new Dimension(240, 28));
        categoryCombo.setPreferredSize(new Dimension(240, 28));
        supplierCombo.setPreferredSize(new Dimension(240, 28));
        descriptionArea.setFont(Theme.BODY);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setPreferredSize(new Dimension(240, 60));

        initialQtyField.setText("0");
        reorderField.setText("10");
        criticalField.setText("3");
        costPriceField.setText("0.00");
        sellingPriceField.setText("0.00");

        // Row 0: Basic Info
        c.gridy = 0;
        c.gridx = 0;
        grid.add(UiSupport.labeled("Product Name *", nameField, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("SKU / Product Code *", skuField, new JLabel(" ")), c);

        // Row 1: Category
        c.gridy = 1;
        c.gridx = 0;
        grid.add(UiSupport.labeled("Category *", categoryCombo, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Or Create New Category", newCategoryField, new JLabel(" ")), c);

        // Row 2: Unit & Barcode
        c.gridy = 2;
        c.gridx = 0;
        grid.add(UiSupport.labeled("Unit of Measurement *", unitCombo, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Barcode (Optional)", barcodeField, new JLabel(" ")), c);

        // Row 3: Quantities & Thresholds
        c.gridy = 3;
        c.gridx = 0;
        grid.add(UiSupport.labeled("Initial Quantity (creates opening movement)", initialQtyField, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Reorder Threshold *", reorderField, new JLabel(" ")), c);

        // Row 4: Critical Threshold & Expiry
        c.gridy = 4;
        c.gridx = 0;
        grid.add(UiSupport.labeled("Critical Stock Threshold *", criticalField, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Expiry Date (YYYY-MM-DD, Optional)", expiryField, new JLabel(" ")), c);

        // Row 5: Pricing
        c.gridy = 5;
        c.gridx = 0;
        grid.add(UiSupport.labeled("Cost Price ($) *", costPriceField, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Selling Price ($) *", sellingPriceField, new JLabel(" ")), c);

        // Row 6: Location & Supplier
        c.gridy = 6;
        c.gridx = 0;
        grid.add(UiSupport.labeled("Warehouse / Zone (e.g. WH-A)", warehouseField, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Rack / Shelf (e.g. R1-S2)", rackField, new JLabel(" ")), c);

        // Row 7: Supplier & Description
        c.gridy = 7;
        c.gridx = 0;
        grid.add(UiSupport.labeled("Preferred Supplier", supplierCombo, new JLabel(" ")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Description", descScroll, new JLabel(" ")), c);

        formCard.add(grid, BorderLayout.CENTER);

        JPanel actionsBar = new JPanel(new BorderLayout());
        actionsBar.setOpaque(false);
        actionsBar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        statusMessage.setFont(Theme.SMALL);
        actionsBar.add(statusMessage, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(viewCatalogButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(submitButton);
        actionsBar.add(buttonPanel, BorderLayout.EAST);

        formCard.add(actionsBar, BorderLayout.SOUTH);

        JScrollPane scrollForm = new JScrollPane(formCard);
        scrollForm.setBorder(BorderFactory.createEmptyBorder());
        scrollForm.setOpaque(false);
        scrollForm.getViewport().setOpaque(false);

        root.add(header, BorderLayout.NORTH);
        root.add(scrollForm, BorderLayout.CENTER);

        submitButton.addActionListener(e -> registerProduct());
        clearButton.addActionListener(e -> clearForm());
        viewCatalogButton.addActionListener(e -> frame.showPage("products"));
    }

    @Override
    public String id() {
        return "register";
    }

    @Override
    public String title() {
        return "Register Product";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        new SwingWorker<Void, Void>() {
            private List<Category> categories;
            private List<Supplier> suppliers;

            @Override
            protected Void doInBackground() {
                categories = AppContext.get().products().categories();
                suppliers = AppContext.get().suppliers().list(true);
                return null;
            }

            @Override
            protected void done() {
                categoryCombo.removeAllItems();
                categoryCombo.addItem(new CategoryItem(null, "-- Select Category --"));
                if (categories != null) {
                    for (Category cat : categories) {
                        categoryCombo.addItem(new CategoryItem(cat.getId(), cat.getName()));
                    }
                }

                supplierCombo.removeAllItems();
                supplierCombo.addItem(new SupplierItem(null, "-- None / General --"));
                if (suppliers != null) {
                    for (Supplier s : suppliers) {
                        supplierCombo.addItem(new SupplierItem(s.getId(), s.getCompanyName()));
                    }
                }
            }
        }.execute();
    }

    private void registerProduct() {
        submitButton.setEnabled(false);
        statusMessage.setText("Saving product…");
        statusMessage.setForeground(Theme.INFO);

        try {
            ProductRegistrationRequest req = new ProductRegistrationRequest();
            String name = nameField.getText().trim();
            String sku = skuField.getText().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Product name is required.");
            if (sku.isEmpty()) throw new IllegalArgumentException("SKU is required.");

            req.setName(name);
            req.setSku(sku);

            String newCat = newCategoryField.getText().trim();
            if (!newCat.isEmpty()) {
                req.setNewCategoryName(newCat);
            } else {
                CategoryItem selectedCat = (CategoryItem) categoryCombo.getSelectedItem();
                if (selectedCat == null || selectedCat.id == null) {
                    throw new IllegalArgumentException("Please select or enter a Category.");
                }
                req.setCategoryId(selectedCat.id);
            }

            Object selectedUnit = unitCombo.getSelectedItem();
            if (selectedUnit == null || selectedUnit.toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Unit of measurement is required.");
            }
            req.setUnit(selectedUnit.toString().trim());

            String barcode = barcodeField.getText().trim();
            req.setBarcode(barcode.isEmpty() ? null : barcode);

            String initialQtyStr = initialQtyField.getText().trim();
            int initialQty = initialQtyStr.isEmpty() ? 0 : Integer.parseInt(initialQtyStr);
            if (initialQty < 0) throw new IllegalArgumentException("Initial quantity cannot be negative.");
            req.setInitialQuantity(initialQty);

            String reorderStr = reorderField.getText().trim();
            int reorder = reorderStr.isEmpty() ? 0 : Integer.parseInt(reorderStr);
            if (reorder < 0) throw new IllegalArgumentException("Reorder threshold cannot be negative.");
            req.setReorderThreshold(reorder);

            String criticalStr = criticalField.getText().trim();
            int critical = criticalStr.isEmpty() ? 0 : Integer.parseInt(criticalStr);
            if (critical < 0) throw new IllegalArgumentException("Critical threshold cannot be negative.");
            if (critical > reorder && reorder > 0) {
                throw new IllegalArgumentException("Critical threshold cannot exceed reorder threshold.");
            }
            req.setCriticalThreshold(critical);

            String expiry = expiryField.getText().trim();
            req.setExpiryDate(expiry.isEmpty() ? null : expiry);

            req.setCostPriceCents(Money.toCents(costPriceField.getText().trim()));
            req.setSellingPriceCents(Money.toCents(sellingPriceField.getText().trim()));

            String wh = warehouseField.getText().trim();
            req.setWarehouseLocation(wh.isEmpty() ? null : wh);
            String rack = rackField.getText().trim();
            req.setRackShelf(rack.isEmpty() ? null : rack);

            SupplierItem selectedSupplier = (SupplierItem) supplierCombo.getSelectedItem();
            if (selectedSupplier != null && selectedSupplier.id != null) {
                req.setPreferredSupplierId(selectedSupplier.id);
            }

            String desc = descriptionArea.getText().trim();
            req.setDescription(desc.isEmpty() ? null : desc);

            User user = AppContext.get().auth().currentUser();

            new SwingWorker<Product, Void>() {
                @Override
                protected Product doInBackground() {
                    return AppContext.get().products().register(user, req);
                }

                @Override
                protected void done() {
                    submitButton.setEnabled(true);
                    try {
                        Product created = get();
                        statusMessage.setText("Registered: " + created.getSku() + " — " + created.getName());
                        statusMessage.setForeground(Theme.SUCCESS);
                        UiSupport.info(root, "Product successfully registered!\nSKU: "
                                + created.getSku() + "\nOpening Stock: " + created.getQuantity() + " " + created.getUnit());
                        clearForm();
                        refresh();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        statusMessage.setText("Error: " + cause.getMessage());
                        statusMessage.setForeground(Theme.DANGER);
                        UiSupport.error(root, cause.getMessage());
                    }
                }
            }.execute();

        } catch (NumberFormatException nfe) {
            submitButton.setEnabled(true);
            statusMessage.setText("Invalid number format in numeric fields.");
            statusMessage.setForeground(Theme.DANGER);
            UiSupport.error(root, "Please enter valid numeric values for quantities, thresholds, and prices.");
        } catch (Exception ex) {
            submitButton.setEnabled(true);
            statusMessage.setText(ex.getMessage());
            statusMessage.setForeground(Theme.DANGER);
            UiSupport.error(root, ex.getMessage());
        }
    }

    private void clearForm() {
        nameField.setText("");
        skuField.setText("");
        newCategoryField.setText("");
        barcodeField.setText("");
        initialQtyField.setText("0");
        reorderField.setText("10");
        criticalField.setText("3");
        expiryField.setText("");
        costPriceField.setText("0.00");
        sellingPriceField.setText("0.00");
        warehouseField.setText("");
        rackField.setText("");
        descriptionArea.setText("");
        if (categoryCombo.getItemCount() > 0) categoryCombo.setSelectedIndex(0);
        if (supplierCombo.getItemCount() > 0) supplierCombo.setSelectedIndex(0);
        statusMessage.setText(" ");
    }

    private static class CategoryItem {
        final Long id;
        final String name;

        CategoryItem(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class SupplierItem {
        final Long id;
        final String name;

        SupplierItem(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
