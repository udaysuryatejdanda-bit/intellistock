package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.AlertSeverity;
import com.intellistock.model.AlertStatus;
import com.intellistock.model.InventoryAlert;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.MetricCard;
import com.intellistock.ui.components.StatusBadge;
import com.intellistock.ui.components.UiSupport;
import com.intellistock.util.Dates;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class AlertsPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final MetricCard criticalCard = new MetricCard("Critical Alerts", Theme.DANGER);
    private final MetricCard warningCard = new MetricCard("Warnings", Theme.WARNING);
    private final MetricCard infoCard = new MetricCard("Information / Reorders", Theme.INFO);
    private final MetricCard activeTotalCard = new MetricCard("Active Alerts Total", Theme.SIDEBAR_ACTIVE);

    private final JComboBox<String> severityFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JButton recalculateButton = Theme.secondary("Recalculate Alerts");
    private final JButton acknowledgeButton = Theme.secondary("Acknowledge");
    private final JButton resolveButton = Theme.primary("Resolve Selected");
    private final JButton refreshButton = Theme.secondary("Refresh");

    private final DefaultTableModel tableModel = UiSupport.model(
            "ID", "Severity", "Alert Type", "Message", "Rule / Explanation", "Status", "Created At"
    );
    private final JTable table = UiSupport.table(tableModel);

    private List<InventoryAlert> loadedAlerts = new ArrayList<>();

    public AlertsPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Smart Stock Alerts"), BorderLayout.WEST);
        header.add(Theme.muted("Automated rule-based alerts evaluated from live SQLite quantities, expiry dates, and moving-average demand."),
                BorderLayout.SOUTH);

        // Metrics Row
        JPanel metrics = new JPanel(new GridLayout(1, 4, 10, 10));
        metrics.setOpaque(false);
        metrics.add(criticalCard);
        metrics.add(warningCard);
        metrics.add(infoCard);
        metrics.add(activeTotalCard);

        // Filter / Action Bar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        left.setOpaque(false);
        severityFilter.setPreferredSize(new Dimension(130, 28));
        severityFilter.addItem("All Severities");
        severityFilter.addItem("CRITICAL");
        severityFilter.addItem("WARNING");
        severityFilter.addItem("INFO");

        statusFilter.setPreferredSize(new Dimension(140, 28));
        statusFilter.addItem("Active & Ack");
        statusFilter.addItem("ACTIVE");
        statusFilter.addItem("ACKNOWLEDGED");
        statusFilter.addItem("RESOLVED");
        statusFilter.addItem("All Statuses");

        left.add(new JLabel("Severity:"));
        left.add(severityFilter);
        left.add(new JLabel("Status:"));
        left.add(statusFilter);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        right.add(recalculateButton);
        right.add(acknowledgeButton);
        right.add(resolveButton);
        right.add(refreshButton);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        // Table Panel
        JPanel tableCard = new JPanel(new BorderLayout());
        Theme.card(tableCard);

        // Renderers
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                if (value instanceof AlertSeverity sev) {
                    JLabel b = StatusBadge.of(sev);
                    b.setOpaque(true);
                    return b;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        tableCard.add(UiSupport.scroll(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(metrics, BorderLayout.CENTER);
        top.add(toolbar, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(tableCard, BorderLayout.CENTER);

        severityFilter.addActionListener(e -> applyFilter());
        statusFilter.addActionListener(e -> applyFilter());
        refreshButton.addActionListener(e -> refresh());
        recalculateButton.addActionListener(e -> recalculate());
        acknowledgeButton.addActionListener(e -> acknowledgeSelected());
        resolveButton.addActionListener(e -> resolveSelected());
    }

    @Override
    public String id() {
        return "alerts";
    }

    @Override
    public String title() {
        return "Alerts";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        new SwingWorker<List<InventoryAlert>, Void>() {
            @Override
            protected List<InventoryAlert> doInBackground() {
                return AppContext.get().alerts().allAlerts();
            }

            @Override
            protected void done() {
                try {
                    loadedAlerts = get();
                    updateMetrics();
                    applyFilter();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void updateMetrics() {
        long crit = loadedAlerts.stream().filter(a -> a.getStatus() != AlertStatus.RESOLVED && a.getSeverity() == AlertSeverity.CRITICAL).count();
        long warn = loadedAlerts.stream().filter(a -> a.getStatus() != AlertStatus.RESOLVED && a.getSeverity() == AlertSeverity.WARNING).count();
        long info = loadedAlerts.stream().filter(a -> a.getStatus() != AlertStatus.RESOLVED && a.getSeverity() == AlertSeverity.INFO).count();
        long active = crit + warn + info;

        criticalCard.setValue(String.valueOf(crit));
        warningCard.setValue(String.valueOf(warn));
        infoCard.setValue(String.valueOf(info));
        activeTotalCard.setValue(String.valueOf(active));
    }

    private void applyFilter() {
        String sev = (String) severityFilter.getSelectedItem();
        String stat = (String) statusFilter.getSelectedItem();

        tableModel.setRowCount(0);
        for (InventoryAlert a : loadedAlerts) {
            if (sev != null && !"All Severities".equals(sev)) {
                if (a.getSeverity() == null || !sev.equalsIgnoreCase(a.getSeverity().name())) continue;
            }

            if ("Active & Ack".equals(stat)) {
                if (a.getStatus() == AlertStatus.RESOLVED) continue;
            } else if (stat != null && !"All Statuses".equals(stat)) {
                if (a.getStatus() == null || !stat.equalsIgnoreCase(a.getStatus().name())) continue;
            }

            tableModel.addRow(new Object[]{
                    a.getId(),
                    a.getSeverity(),
                    a.getAlertType(),
                    a.getMessage(),
                    a.getExplanation() != null ? a.getExplanation() : "—",
                    a.getStatus(),
                    Dates.formatDisplay(a.getCreatedAt())
            });
        }
    }

    private Long getSelectedAlertId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiSupport.error(root, "Please select an alert from the table.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        return (Long) tableModel.getValueAt(modelRow, 0);
    }

    private void recalculate() {
        recalculateButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                AppContext.get().alerts().refresh();
                return null;
            }

            @Override
            protected void done() {
                recalculateButton.setEnabled(true);
                refresh();
                UiSupport.info(root, "Alert engine completed rule re-evaluation against all inventory items.");
            }
        }.execute();
    }

    private void acknowledgeSelected() {
        Long id = getSelectedAlertId();
        if (id == null) return;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                AppContext.get().alerts().acknowledge(id);
                return null;
            }

            @Override
            protected void done() {
                refresh();
            }
        }.execute();
    }

    private void resolveSelected() {
        Long id = getSelectedAlertId();
        if (id == null) return;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                AppContext.get().alerts().resolve(id);
                return null;
            }

            @Override
            protected void done() {
                refresh();
            }
        }.execute();
    }
}
