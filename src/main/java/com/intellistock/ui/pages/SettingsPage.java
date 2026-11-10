package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.Role;
import com.intellistock.model.User;
import com.intellistock.repository.SettingsRepository;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.UiSupport;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;

public class SettingsPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final JTextField demandWindowField = UiSupport.field();
    private final JTextField demandRatioField = UiSupport.field();
    private final JTextField expiryDaysField = UiSupport.field();
    private final JTextField leadDaysField = UiSupport.field();
    private final JButton saveSettingsButton = Theme.primary("Save Settings");

    private final JLabel backupPathLabel = new JLabel("Backup directory: —");
    private final JButton backupNowButton = Theme.primary("Create Backup Now");
    private final JButton restoreBackupButton = Theme.secondary("Restore from Backup File");

    private final JButton resetDemoButton = Theme.secondary("Reset Demo Database");

    private final SettingsRepository settingsRepo = new SettingsRepository();

    public SettingsPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Settings & Maintenance"), BorderLayout.WEST);
        header.add(Theme.muted("Configure operational thresholds, manage SQLite backups, and inspect runtime metadata."),
                BorderLayout.SOUTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Section 1: Rules & Thresholds
        JPanel rulesCard = new JPanel(new BorderLayout(10, 10));
        Theme.card(rulesCard);
        rulesCard.add(Theme.title("Smart Demand & Alert Parameters"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 8, 6, 8);
        c.weightx = 0.5;

        c.gridy = 0; c.gridx = 0;
        grid.add(UiSupport.labeled("Demand Analysis Window (Days)", demandWindowField,
                new JLabel("Number of historical days for moving average")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("High Demand Threshold Ratio", demandRatioField,
                new JLabel("e.g. 1.5 means 50% above baseline")), c);

        c.gridy = 1; c.gridx = 0;
        grid.add(UiSupport.labeled("Expiry Warning Window (Days)", expiryDaysField,
                new JLabel("Flag products expiring within this timeframe")), c);
        c.gridx = 1;
        grid.add(UiSupport.labeled("Supplier Lead Time (Days)", leadDaysField,
                new JLabel("Default expected lead time for stock reorders")), c);

        JPanel rulesActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rulesActions.setOpaque(false);
        rulesActions.add(saveSettingsButton);

        rulesCard.add(grid, BorderLayout.CENTER);
        rulesCard.add(rulesActions, BorderLayout.SOUTH);

        // Section 2: Backup & Restore
        JPanel backupCard = new JPanel(new BorderLayout(10, 10));
        Theme.card(backupCard);
        backupCard.add(Theme.title("Database Backup & Disaster Recovery"), BorderLayout.NORTH);

        JPanel backupInfo = new JPanel(new BorderLayout(4, 4));
        backupInfo.setOpaque(false);
        backupPathLabel.setFont(Theme.SMALL);
        backupInfo.add(backupPathLabel, BorderLayout.NORTH);
        backupInfo.add(Theme.muted("IntelliStock AI uses SQLite WAL checkpointing and atomic VACUUM INTO for online, zero-downtime backups."),
                BorderLayout.SOUTH);

        JPanel backupActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        backupActions.setOpaque(false);
        backupActions.add(backupNowButton);
        backupActions.add(restoreBackupButton);

        backupCard.add(backupInfo, BorderLayout.CENTER);
        backupCard.add(backupActions, BorderLayout.SOUTH);

        // Section 3: Demo Data & Reset
        JPanel maintenanceCard = new JPanel(new BorderLayout(10, 10));
        Theme.card(maintenanceCard);
        maintenanceCard.add(Theme.title("Demonstration Data Reset"), BorderLayout.NORTH);

        JPanel maintBody = new JPanel(new BorderLayout(4, 4));
        maintBody.setOpaque(false);
        maintBody.add(Theme.muted("Resetting restores the initial demonstration users, categories, suppliers, and products with pre-configured stock movements."),
                BorderLayout.NORTH);

        JPanel maintActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        maintActions.setOpaque(false);
        maintActions.add(resetDemoButton);

        maintenanceCard.add(maintBody, BorderLayout.CENTER);
        maintenanceCard.add(maintActions, BorderLayout.SOUTH);

        // Section 4: System Information
        JPanel aboutCard = new JPanel(new BorderLayout(6, 6));
        Theme.card(aboutCard);
        aboutCard.add(Theme.title("System & Environment Information"), BorderLayout.NORTH);

        String infoHtml = "<html><body style='font-family: Segoe UI, sans-serif; font-size: 11px;'>"
                + "<b>Application:</b> IntelliStock AI v1.0.0<br>"
                + "<b>Java Runtime:</b> " + System.getProperty("java.runtime.version") + " (" + System.getProperty("java.vendor") + ")<br>"
                + "<b>Operating System:</b> " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")<br>"
                + "<b>Database Engine:</b> SQLite Relational Storage (JDBC)<br>"
                + "<b>Database File:</b> " + AppContext.get().database().path().toAbsolutePath() + "<br>"
                + "<b>Storage Mode:</b> ACID Compliant Persistent Local Relational Database<br>"
                + "<b>Architecture:</b> 3-Tier Layered Architecture (Entities -> JDBC Repositories -> Business Services -> Swing UI)"
                + "</body></html>";
        JLabel aboutLabel = new JLabel(infoHtml);
        aboutCard.add(aboutLabel, BorderLayout.CENTER);

        content.add(rulesCard);
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(backupCard);
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(maintenanceCard);
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(aboutCard);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);

        saveSettingsButton.addActionListener(e -> saveSettings());
        backupNowButton.addActionListener(e -> createBackup());
        restoreBackupButton.addActionListener(e -> restoreBackup());
        resetDemoButton.addActionListener(e -> resetDemo());
    }

    @Override
    public String id() {
        return "settings";
    }

    @Override
    public String title() {
        return "Settings";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        backupPathLabel.setText("Backup directory: " + AppContext.get().backups().backupDestination().toAbsolutePath());

        new SwingWorker<Void, Void>() {
            private String window;
            private String ratio;
            private String expiry;
            private String lead;

            @Override
            protected Void doInBackground() {
                AppContext.get().database().execute(connection -> {
                    window = settingsRepo.get(connection, "demand.window.days").orElse("14");
                    ratio = settingsRepo.get(connection, "demand.high.ratio").orElse("1.5");
                    expiry = settingsRepo.get(connection, "alerts.expiry.days").orElse("30");
                    lead = settingsRepo.get(connection, "supplier.lead.days").orElse("7");
                });
                return null;
            }

            @Override
            protected void done() {
                demandWindowField.setText(window != null ? window : "14");
                demandRatioField.setText(ratio != null ? ratio : "1.5");
                expiryDaysField.setText(expiry != null ? expiry : "30");
                leadDaysField.setText(lead != null ? lead : "7");
            }
        }.execute();
    }

    private void saveSettings() {
        try {
            int window = Integer.parseInt(demandWindowField.getText().trim());
            double ratio = Double.parseDouble(demandRatioField.getText().trim());
            int expiry = Integer.parseInt(expiryDaysField.getText().trim());
            int lead = Integer.parseInt(leadDaysField.getText().trim());

            if (window <= 0 || ratio <= 0 || expiry <= 0 || lead <= 0) {
                throw new IllegalArgumentException("All configuration parameters must be positive numbers.");
            }

            AppContext.get().database().execute(connection -> {
                settingsRepo.put(connection, "demand.window.days", String.valueOf(window));
                settingsRepo.put(connection, "demand.high.ratio", String.valueOf(ratio));
                settingsRepo.put(connection, "alerts.expiry.days", String.valueOf(expiry));
                settingsRepo.put(connection, "supplier.lead.days", String.valueOf(lead));
            });

            AppContext.get().alerts().refresh();
            UiSupport.info(root, "Settings saved successfully! Smart alert rules recalculated.");
        } catch (NumberFormatException ex) {
            UiSupport.error(root, "Please enter valid numeric values for all fields.");
        } catch (Exception ex) {
            UiSupport.error(root, "Failed to save settings: " + ex.getMessage());
        }
    }

    private void createBackup() {
        User user = AppContext.get().auth().currentUser();
        if (user.getRole() != Role.ADMIN) {
            UiSupport.error(root, "Only administrators can create database backups.");
            return;
        }

        backupNowButton.setEnabled(false);
        new SwingWorker<Path, Void>() {
            @Override
            protected Path doInBackground() {
                return AppContext.get().backups().createBackup(user);
            }

            @Override
            protected void done() {
                backupNowButton.setEnabled(true);
                try {
                    Path backupPath = get();
                    UiSupport.info(root, "Database backup created successfully!\nLocation:\n" + backupPath.toAbsolutePath());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    UiSupport.error(root, "Backup failed: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void restoreBackup() {
        User user = AppContext.get().auth().currentUser();
        if (user.getRole() != Role.ADMIN) {
            UiSupport.error(root, "Only administrators can restore database backups.");
            return;
        }

        JFileChooser chooser = new JFileChooser(AppContext.get().backups().backupDestination().toFile());
        chooser.setDialogTitle("Select IntelliStock SQLite Backup File");
        chooser.setFileFilter(new FileNameExtensionFilter("SQLite Database Files (*.db)", "db"));
        int ret = chooser.showOpenDialog(frame);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (!UiSupport.confirm(root, "WARNING: Restoring will overwrite the current live database with:\n"
                    + selected.getAbsolutePath() + "\n\nA safety backup of current data will be made first. Continue?")) {
                return;
            }

            try {
                AppContext.get().backups().restore(user, selected.toPath());
                UiSupport.info(root, "Database restored successfully! Reloading views.");
                frame.refreshAll();
            } catch (Exception ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                UiSupport.error(root, "Restore failed: " + cause.getMessage());
            }
        }
    }

    private void resetDemo() {
        if (!UiSupport.confirm(root, "Are you sure you want to reset the demonstration database?\n"
                + "This will delete all custom records and re-seed the standard demonstration catalog.")) {
            return;
        }

        try {
            AppContext.initialize(true);
            UiSupport.info(root, "Demonstration database reset successfully!");
            frame.refreshAll();
        } catch (Exception ex) {
            UiSupport.error(root, "Reset failed: " + ex.getMessage());
        }
    }
}
