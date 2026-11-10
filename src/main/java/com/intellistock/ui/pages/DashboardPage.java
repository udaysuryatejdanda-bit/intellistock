package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.InventoryAlert;
import com.intellistock.model.StockMovement;
import com.intellistock.service.DashboardService;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.BarChartPanel;
import com.intellistock.ui.components.MetricCard;
import com.intellistock.ui.components.UiSupport;
import com.intellistock.util.Dates;
import com.intellistock.util.Money;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class DashboardPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));
    private final MetricCard products = new MetricCard("Registered products", Theme.INFO);
    private final MetricCard quantity = new MetricCard("Total quantity", Theme.SIDEBAR_ACTIVE);
    private final MetricCard value = new MetricCard("Inventory valuation", Theme.SUCCESS);
    private final MetricCard low = new MetricCard("Low stock", Theme.WARNING);
    private final MetricCard critical = new MetricCard("Critical stock", Theme.DANGER);
    private final MetricCard out = new MetricCard("Out of stock", Theme.MUTED);
    private final MetricCard pending = new MetricCard("Pending purchase orders", Theme.WARNING);
    private final MetricCard expiry = new MetricCard("Nearing expiry", Theme.DANGER);
    private final BarChartPanel movementChart = new BarChartPanel();
    private final BarChartPanel demandChart = new BarChartPanel();
    private final DefaultTableModel movementModel = UiSupport.model("When", "Product", "Type", "Change", "Balance");
    private final DefaultTableModel alertModel = UiSupport.model("Severity", "Message", "Status");
    private final DefaultTableModel reorderModel = UiSupport.model("SKU", "Product", "Suggested qty", "Why");

    public DashboardPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Dashboard"), BorderLayout.WEST);
        header.add(Theme.muted("Metrics are calculated from live SQLite records."), BorderLayout.SOUTH);
        JPanel metrics = new JPanel(new GridLayout(2, 4, 10, 10));
        metrics.setOpaque(false);
        metrics.add(products);
        metrics.add(quantity);
        metrics.add(value);
        metrics.add(pending);
        metrics.add(low);
        metrics.add(critical);
        metrics.add(out);
        metrics.add(expiry);

        JPanel charts = new JPanel(new GridLayout(1, 2, 10, 10));
        charts.setOpaque(false);
        charts.add(wrapChart("Stock movement volume (7 days)", movementChart));
        charts.add(wrapChart("Recent outbound demand by SKU", demandChart));

        JPanel tables = new JPanel(new GridLayout(1, 3, 10, 10));
        tables.setOpaque(false);
        tables.add(wrapTable("Recent stock movements", UiSupport.table(movementModel)));
        tables.add(wrapTable("Recent alerts", UiSupport.table(alertModel)));
        tables.add(wrapTable("Reorder recommendations", UiSupport.table(reorderModel)));

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(metrics, BorderLayout.NORTH);
        center.add(charts, BorderLayout.CENTER);
        center.add(tables, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
    }

    @Override
    public String id() {
        return "dashboard";
    }

    @Override
    public String title() {
        return "Dashboard";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        new SwingWorker<DashboardService.DashboardSnapshot, Void>() {
            @Override
            protected DashboardService.DashboardSnapshot doInBackground() {
                return AppContext.get().dashboard().snapshot();
            }

            @Override
            protected void done() {
                try {
                    bind(get());
                } catch (Exception ignored) {
                    // Keep previous values if refresh fails.
                }
            }
        }.execute();
    }

    private void bind(DashboardService.DashboardSnapshot snap) {
        products.setValue(String.valueOf(snap.totalProducts));
        quantity.setValue(String.valueOf(snap.totalQuantity));
        value.setValue(Money.format(snap.valuationCents));
        low.setValue(String.valueOf(snap.lowStock));
        critical.setValue(String.valueOf(snap.criticalStock));
        out.setValue(String.valueOf(snap.outOfStock));
        pending.setValue(String.valueOf(snap.pendingOrders));
        expiry.setValue(String.valueOf(snap.nearingExpiry));
        movementChart.setData(snap.movementTrend, "No stock movements in the last 7 days.");
        demandChart.setData(snap.demandTrend, "No outbound demand yet.");
        movementModel.setRowCount(0);
        if (snap.recentMovements.isEmpty()) {
            movementModel.addRow(new Object[]{"—", "No movements recorded", "", "", ""});
        } else {
            for (StockMovement movement : snap.recentMovements) {
                movementModel.addRow(new Object[]{
                        Dates.formatDisplay(movement.getOccurredAt()), movement.getProductName(),
                        movement.getMovementType(), movement.getQuantityDelta(), movement.getResultingQuantity()
                });
            }
        }
        alertModel.setRowCount(0);
        if (snap.openAlerts.isEmpty()) {
            alertModel.addRow(new Object[]{"—", "No active alerts", ""});
        } else {
            for (InventoryAlert alert : snap.openAlerts.stream().limit(8).toList()) {
                alertModel.addRow(new Object[]{alert.getSeverity(), alert.getMessage(), alert.getStatus()});
            }
        }
        reorderModel.setRowCount(0);
        if (snap.reorderRecommendations.isEmpty()) {
            reorderModel.addRow(new Object[]{"—", "No reorder suggestions", "", "Need low stock plus demand history"});
        } else {
            snap.reorderRecommendations.stream().limit(8).forEach(row ->
                    reorderModel.addRow(new Object[]{
                            row.product.getSku(), row.product.getName(), row.suggestedReorder, row.explanation
                    }));
        }
        frame.getRootPane().revalidate();
    }

    private JPanel wrapChart(String title, JComponent chart) {
        JPanel panel = new JPanel(new BorderLayout());
        Theme.card(panel);
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(chart, BorderLayout.CENTER);
        return panel;
    }

    private JPanel wrapTable(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        Theme.card(panel);
        panel.add(new JLabel(title), BorderLayout.NORTH);
        JScrollPane scroll = UiSupport.scroll(table);
        scroll.setPreferredSize(new java.awt.Dimension(200, 180));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
}
