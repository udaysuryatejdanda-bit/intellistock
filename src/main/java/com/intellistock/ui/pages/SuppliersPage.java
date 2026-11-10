package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.Product;
import com.intellistock.model.Supplier;
import com.intellistock.model.User;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.UiSupport;
import com.intellistock.util.Money;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class SuppliersPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final JTextField searchField = UiSupport.field();
    private final JCheckBox activeOnlyCheck = new JCheckBox("Active Only", true);
    private final JButton addSupplierButton = Theme.primary("+ Add Supplier");
    private final JButton editSupplierButton = Theme.secondary("Edit Selected");
    private final JButton refreshButton = Theme.secondary("Refresh");

    private final DefaultTableModel supplierModel = UiSupport.model(
            "ID", "Company Name", "Contact Person", "Email", "Phone", "Address", "Status"
    );
    private final JTable supplierTable = UiSupport.table(supplierModel);

    private final DefaultTableModel productModel = UiSupport.model(
            "SKU", "Product Name", "Current Stock", "Unit", "Cost Price", "Stock Status"
    );
    private final JTable productTable = UiSupport.table(productModel);
    private final JLabel detailTitle = new JLabel("Products linked to selected supplier");

    private List<Supplier> loadedSuppliers = new ArrayList<>();

    public SuppliersPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Suppliers"), BorderLayout.WEST);
        header.add(Theme.muted("Manage wholesale vendors, contact details, and catalog associations."),
                BorderLayout.SOUTH);

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        left.setOpaque(false);
        searchField.setPreferredSize(new Dimension(200, 28));
        left.add(new JLabel("Search:"));
        left.add(searchField);
        activeOnlyCheck.setOpaque(false);
        left.add(activeOnlyCheck);
        JButton searchBtn = Theme.secondary("Search");
        searchBtn.addActionListener(e -> applyFilter());
        left.add(searchBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        right.add(editSupplierButton);
        right.add(refreshButton);
        right.add(addSupplierButton);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        // Top table: Suppliers
        JPanel supplierCard = new JPanel(new BorderLayout());
        Theme.card(supplierCard);
        supplierCard.add(UiSupport.scroll(supplierTable), BorderLayout.CENTER);

        // Bottom table: Linked Products
        JPanel productCard = new JPanel(new BorderLayout(6, 6));
        Theme.card(productCard);
        detailTitle.setFont(Theme.HEADING);
        productCard.add(detailTitle, BorderLayout.NORTH);
        productCard.add(UiSupport.scroll(productTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, supplierCard, productCard);
        splitPane.setResizeWeight(0.55);
        splitPane.setOpaque(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);

        supplierTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadProductsForSelected();
            }
        });

        addSupplierButton.addActionListener(e -> openSupplierDialog(new Supplier()));
        editSupplierButton.addActionListener(e -> {
            Supplier s = getSelectedSupplier();
            if (s != null) openSupplierDialog(s);
        });
        refreshButton.addActionListener(e -> refresh());
        activeOnlyCheck.addActionListener(e -> refresh());
        searchField.addActionListener(e -> applyFilter());
    }

    @Override
    public String id() {
        return "suppliers";
    }

    @Override
    public String title() {
        return "Suppliers";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        boolean activeOnly = activeOnlyCheck.isSelected();
        new SwingWorker<List<Supplier>, Void>() {
            @Override
            protected List<Supplier> doInBackground() {
                return AppContext.get().suppliers().list(activeOnly);
            }

            @Override
            protected void done() {
                try {
                    loadedSuppliers = get();
                    applyFilter();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String query = searchField.getText().trim().toLowerCase();
        supplierModel.setRowCount(0);
        for (Supplier s : loadedSuppliers) {
            if (!query.isEmpty()) {
                boolean matchName = s.getCompanyName() != null && s.getCompanyName().toLowerCase().contains(query);
                boolean matchContact = s.getContactPerson() != null && s.getContactPerson().toLowerCase().contains(query);
                boolean matchEmail = s.getEmail() != null && s.getEmail().toLowerCase().contains(query);
                if (!matchName && !matchContact && !matchEmail) continue;
            }

            supplierModel.addRow(new Object[]{
                    s.getId(),
                    s.getCompanyName(),
                    s.getContactPerson() != null ? s.getContactPerson() : "—",
                    s.getEmail() != null ? s.getEmail() : "—",
                    s.getPhone() != null ? s.getPhone() : "—",
                    s.getAddress() != null ? s.getAddress() : "—",
                    s.isActive() ? "Active" : "Inactive"
            });
        }
        productModel.setRowCount(0);
        detailTitle.setText("Products linked to selected supplier");
    }

    private Supplier getSelectedSupplier() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) {
            UiSupport.error(root, "Please select a supplier from the table.");
            return null;
        }
        int modelRow = supplierTable.convertRowIndexToModel(row);
        long id = (Long) supplierModel.getValueAt(modelRow, 0);
        for (Supplier s : loadedSuppliers) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    private void loadProductsForSelected() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) return;
        int modelRow = supplierTable.convertRowIndexToModel(row);
        long id = (Long) supplierModel.getValueAt(modelRow, 0);
        String comp = (String) supplierModel.getValueAt(modelRow, 1);
        detailTitle.setText("Products supplied by: " + comp);

        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() {
                return AppContext.get().suppliers().productsFor(id);
            }

            @Override
            protected void done() {
                try {
                    List<Product> prods = get();
                    productModel.setRowCount(0);
                    if (prods.isEmpty()) {
                        productModel.addRow(new Object[]{"—", "No linked products registered for this supplier", "", "", "", ""});
                    } else {
                        for (Product p : prods) {
                            productModel.addRow(new Object[]{
                                    p.getSku(),
                                    p.getName(),
                                    p.getQuantity(),
                                    p.getUnit(),
                                    Money.format(p.getCostPriceCents()),
                                    p.getStockStatus()
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void openSupplierDialog(Supplier supplier) {
        boolean isNew = supplier.getId() == 0;
        JDialog dialog = new JDialog(frame, isNew ? "Add New Supplier" : "Edit Supplier: " + supplier.getCompanyName(), true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        Theme.card(form);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 8, 6, 8);
        c.weightx = 1.0;

        JTextField compField = UiSupport.field();
        compField.setText(supplier.getCompanyName() != null ? supplier.getCompanyName() : "");

        JTextField contactField = UiSupport.field();
        contactField.setText(supplier.getContactPerson() != null ? supplier.getContactPerson() : "");

        JTextField emailField = UiSupport.field();
        emailField.setText(supplier.getEmail() != null ? supplier.getEmail() : "");

        JTextField phoneField = UiSupport.field();
        phoneField.setText(supplier.getPhone() != null ? supplier.getPhone() : "");

        JTextField addrField = UiSupport.field();
        addrField.setText(supplier.getAddress() != null ? supplier.getAddress() : "");

        JTextArea notesArea = new JTextArea(3, 20);
        notesArea.setText(supplier.getNotes() != null ? supplier.getNotes() : "");
        JScrollPane notesScroll = new JScrollPane(notesArea);

        JCheckBox activeCheck = new JCheckBox("Supplier is Active", isNew || supplier.isActive());
        activeCheck.setOpaque(false);

        c.gridy = 0; c.gridx = 0;
        form.add(UiSupport.labeled("Company Name *", compField, new JLabel(" ")), c);
        c.gridy = 1;
        form.add(UiSupport.labeled("Contact Person", contactField, new JLabel(" ")), c);
        c.gridy = 2;
        form.add(UiSupport.labeled("Email", emailField, new JLabel(" ")), c);
        c.gridy = 3;
        form.add(UiSupport.labeled("Phone", phoneField, new JLabel(" ")), c);
        c.gridy = 4;
        form.add(UiSupport.labeled("Address", addrField, new JLabel(" ")), c);
        c.gridy = 5;
        form.add(UiSupport.labeled("Notes", notesScroll, new JLabel(" ")), c);
        c.gridy = 6;
        form.add(activeCheck, c);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.setOpaque(false);
        JButton cancelBtn = Theme.secondary("Cancel");
        JButton saveBtn = Theme.primary(isNew ? "Create Supplier" : "Save Changes");

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                String name = compField.getText().trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Company name is required.");

                supplier.setCompanyName(name);
                supplier.setContactPerson(contactField.getText().trim().isEmpty() ? null : contactField.getText().trim());
                supplier.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                supplier.setPhone(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());
                supplier.setAddress(addrField.getText().trim().isEmpty() ? null : addrField.getText().trim());
                supplier.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                supplier.setActive(activeCheck.isSelected());

                User user = AppContext.get().auth().currentUser();
                AppContext.get().suppliers().save(user, supplier);
                UiSupport.info(dialog, "Supplier saved successfully.");
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
}
