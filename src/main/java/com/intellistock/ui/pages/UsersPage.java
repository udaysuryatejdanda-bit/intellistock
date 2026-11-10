package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.Role;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class UsersPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final JButton addUserButton = Theme.primary("+ Add User");
    private final JButton changePasswordButton = Theme.secondary("Change Password");
    private final JButton toggleActiveButton = Theme.secondary("Toggle Active Status");
    private final JButton refreshButton = Theme.secondary("Refresh");

    private final DefaultTableModel tableModel = UiSupport.model(
            "ID", "Username", "Full Name", "Role", "Active Status", "Created At"
    );
    private final JTable table = UiSupport.table(tableModel);

    private List<User> loadedUsers = new ArrayList<>();

    public UsersPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("User Management"), BorderLayout.WEST);
        header.add(Theme.muted("Administrator access: manage staff credentials, roles, and account security."),
                BorderLayout.SOUTH);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        right.add(changePasswordButton);
        right.add(toggleActiveButton);
        right.add(refreshButton);
        right.add(addUserButton);

        toolbar.add(right, BorderLayout.EAST);

        JPanel tableCard = new JPanel(new BorderLayout());
        Theme.card(tableCard);
        tableCard.add(UiSupport.scroll(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(tableCard, BorderLayout.CENTER);

        refreshButton.addActionListener(e -> refresh());
        addUserButton.addActionListener(e -> openAddUserDialog());
        changePasswordButton.addActionListener(e -> openChangePasswordDialog());
        toggleActiveButton.addActionListener(e -> toggleActiveStatus());
    }

    @Override
    public String id() {
        return "users";
    }

    @Override
    public String title() {
        return "Users";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        User actor = AppContext.get().auth().currentUser();
        if (actor == null || actor.getRole() != Role.ADMIN) {
            tableModel.setRowCount(0);
            return;
        }

        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() {
                return AppContext.get().users().list(actor);
            }

            @Override
            protected void done() {
                try {
                    loadedUsers = get();
                    tableModel.setRowCount(0);
                    for (User u : loadedUsers) {
                        tableModel.addRow(new Object[]{
                                u.getId(),
                                u.getUsername(),
                                u.getFullName(),
                                u.getRole(),
                                u.isActive() ? "Active" : "Inactive",
                                Dates.formatDisplay(u.getCreatedAt())
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private User getSelectedUser() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiSupport.error(root, "Please select a user from the table.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        long id = (Long) tableModel.getValueAt(modelRow, 0);
        for (User u : loadedUsers) {
            if (u.getId() == id) return u;
        }
        return null;
    }

    private void openAddUserDialog() {
        User actor = AppContext.get().auth().currentUser();
        JDialog dialog = new JDialog(frame, "Add New User Account", true);
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        Theme.card(form);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 6, 6, 6);
        c.weightx = 1.0;

        JTextField usernameField = UiSupport.field();
        JTextField fullNameField = UiSupport.field();
        JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
        JPasswordField passwordField = new JPasswordField();

        c.gridy = 0; c.gridx = 0;
        form.add(UiSupport.labeled("Username *", usernameField, new JLabel(" ")), c);
        c.gridy = 1;
        form.add(UiSupport.labeled("Full Name *", fullNameField, new JLabel(" ")), c);
        c.gridy = 2;
        form.add(UiSupport.labeled("Role *", roleCombo, new JLabel(" ")), c);
        c.gridy = 3;
        form.add(UiSupport.labeled("Initial Password (min 8 chars) *", passwordField, new JLabel(" ")), c);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        JButton cancelBtn = Theme.secondary("Cancel");
        JButton createBtn = Theme.primary("Create User");

        cancelBtn.addActionListener(e -> dialog.dispose());
        createBtn.addActionListener(e -> {
            try {
                String u = usernameField.getText().trim();
                String fn = fullNameField.getText().trim();
                Role r = (Role) roleCombo.getSelectedItem();
                char[] p = passwordField.getPassword();

                AppContext.get().users().create(actor, u, fn, r, p);
                UiSupport.info(dialog, "User account '" + u + "' created successfully.");
                dialog.dispose();
                refresh();
            } catch (Exception ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                UiSupport.error(dialog, "Failed: " + cause.getMessage());
            }
        });

        bottom.add(cancelBtn);
        bottom.add(createBtn);

        panel.add(form, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void openChangePasswordDialog() {
        User target = getSelectedUser();
        if (target == null) return;
        User actor = AppContext.get().auth().currentUser();

        JDialog dialog = new JDialog(frame, "Change Password: " + target.getUsername(), true);
        dialog.setSize(400, 240);
        dialog.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        Theme.card(form);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 6, 6, 6);
        c.weightx = 1.0;

        JPasswordField newPass = new JPasswordField();
        c.gridy = 0; c.gridx = 0;
        form.add(UiSupport.labeled("New Password (min 8 chars) *", newPass, new JLabel(" ")), c);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        JButton cancelBtn = Theme.secondary("Cancel");
        JButton saveBtn = Theme.primary("Update Password");

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                char[] p = newPass.getPassword();
                AppContext.get().users().changePassword(actor, target.getId(), p);
                UiSupport.info(dialog, "Password updated successfully for " + target.getUsername());
                dialog.dispose();
            } catch (Exception ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                UiSupport.error(dialog, "Failed: " + cause.getMessage());
            }
        });

        bottom.add(cancelBtn);
        bottom.add(saveBtn);

        panel.add(form, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void toggleActiveStatus() {
        User target = getSelectedUser();
        if (target == null) return;
        User actor = AppContext.get().auth().currentUser();

        boolean newActive = !target.isActive();
        String action = newActive ? "activate" : "deactivate";

        if (!UiSupport.confirm(root, "Are you sure you want to " + action + " user '" + target.getUsername() + "'?")) {
            return;
        }

        try {
            AppContext.get().users().setActive(actor, target.getId(), newActive);
            UiSupport.info(root, "User '" + target.getUsername() + "' is now " + (newActive ? "active" : "inactive") + ".");
            refresh();
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            UiSupport.error(root, "Failed: " + cause.getMessage());
        }
    }
}
