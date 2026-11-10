package com.intellistock.ui.pages;

import com.intellistock.analytics.DemandAnalytics;
import com.intellistock.config.AppContext;
import com.intellistock.report.DemandPrintReport;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.MetricCard;
import com.intellistock.ui.components.UiSupport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class DemandPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final MetricCard totalAnalyzedCard = new MetricCard("Total Analyzed", Theme.INFO);
    private final MetricCard highDemandCard = new MetricCard("High Demand Flagged", Theme.DANGER);
    private final MetricCard reorderCard = new MetricCard("Reorder Recommended", Theme.WARNING);
    private final MetricCard windowCard = new MetricCard("Analysis Window", Theme.SIDEBAR_ACTIVE);

    private final JComboBox<String> filterCombo = new JComboBox<>();
    private final JButton refreshButton = Theme.secondary("Run Analysis");
    private final JButton createPoButton = Theme.primary("Create Purchase Order");

    private final DefaultTableModel tableModel = UiSupport.model(
            "SKU", "Product Name", "Stock", "Velocity", "Recent Outbound", "Baseline Outbound",
            "Recent Daily", "Baseline Daily", "High Demand?", "Days Left", "Suggested Reorder", "Analysis Note"
    );
    private final JTable table = UiSupport.table(tableModel);

    private List<DemandAnalytics.ProductDemand> loadedDemand = new ArrayList<>();

    public DemandPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Smart Demand Analytics"), BorderLayout.WEST);
        header.add(Theme.muted("Moving-average outbound consumption analysis and inventory run-rate projections. Evaluated using transparent formulas, not AI/ML."),
                BorderLayout.SOUTH);

        // Metrics Row
        JPanel metrics = new JPanel(new GridLayout(1, 4, 10, 10));
        metrics.setOpaque(false);
        metrics.add(totalAnalyzedCard);
        metrics.add(highDemandCard);
        metrics.add(reorderCard);
        metrics.add(windowCard);

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        left.setOpaque(false);
        filterCombo.setPreferredSize(new Dimension(200, 28));
        filterCombo.addItem("All Products");
        filterCombo.addItem("High Demand Only");
        filterCombo.addItem("Reorder Suggestions Only");
        left.add(new JLabel("Filter View:"));
        left.add(filterCombo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        right.add(refreshButton);
        right.add(createPoButton);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        // Table Panel
        JPanel tableCard = new JPanel(new BorderLayout());
        Theme.card(tableCard);
        tableCard.add(UiSupport.scroll(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(metrics, BorderLayout.CENTER);
        top.add(toolbar, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(tableCard, BorderLayout.CENTER);

        filterCombo.addActionListener(e -> applyFilter());
        refreshButton.addActionListener(e -> refresh());
        createPoButton.addActionListener(e -> frame.showPage("orders"));
    }

    @Override
    public String id() {
        return "demand";
    }

    @Override
    public String title() {
        return "Demand Analysis";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        new SwingWorker<List<DemandAnalytics.ProductDemand>, Void>() {
            private int windowDays;

            @Override
            protected List<DemandAnalytics.ProductDemand> doInBackground() {
                windowDays = AppContext.get().demand().windowDays();
                return AppContext.get().demand().analyze();
            }

            @Override
            protected void done() {
                try {
                    loadedDemand = get();
                    windowCard.setValue(windowDays + " Days");
                    updateMetrics();
                    applyFilter();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void updateMetrics() {
        long high = loadedDemand.stream().filter(d -> d.highDemand).count();
        long reorder = loadedDemand.stream().filter(d -> d.suggestedReorder != null && d.suggestedReorder > 0).count();

        totalAnalyzedCard.setValue(String.valueOf(loadedDemand.size()));
        highDemandCard.setValue(String.valueOf(high));
        reorderCard.setValue(String.valueOf(reorder));
    }

    private void applyFilter() {
        String filter = (String) filterCombo.getSelectedItem();
        tableModel.setRowCount(0);

        for (DemandAnalytics.ProductDemand d : loadedDemand) {
            if ("High Demand Only".equals(filter) && !d.highDemand) continue;
            if ("Reorder Suggestions Only".equals(filter) && (d.suggestedReorder == null || d.suggestedReorder <= 0)) continue;

            tableModel.addRow(new Object[]{
                    d.product.getSku(),
                    d.product.getName(),
                    d.product.getQuantity(),
                    d.velocity,
                    d.recentOutbound,
                    d.previousOutbound,
                    String.format("%.2f", d.recentDailyAverage),
                    String.format("%.2f", d.baselineDailyAverage),
                    d.highDemand ? "YES" : "No",
                    d.daysRemaining != null ? String.format("%.1f days", d.daysRemaining) : "N/A",
                    d.suggestedReorder != null && d.suggestedReorder > 0 ? d.suggestedReorder : "—",
                    d.explanation
            });
        }
    }
}
