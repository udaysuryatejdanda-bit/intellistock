package com.intellistock.ui.pages;

import com.intellistock.config.AppContext;
import com.intellistock.model.StockStatus;
import com.intellistock.ui.MainFrame;
import com.intellistock.ui.Page;
import com.intellistock.ui.Theme;
import com.intellistock.ui.components.UiSupport;
import com.intellistock.util.Dates;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReportsPage implements Page {
    private final MainFrame frame;
    private final JPanel root = new JPanel(new BorderLayout(12, 12));

    private final JComboBox<ReportOption> reportSelector = new JComboBox<>();
    private final JButton previewButton = Theme.secondary("Generate Preview");
    private final JButton exportButton = Theme.primary("Export to CSV");
    private final JLabel statusLabel = new JLabel("Select a report and click Generate.");

    private final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = UiSupport.table(tableModel);

    private List<String[]> currentData = new ArrayList<>();

    public ReportsPage(MainFrame frame) {
        this.frame = frame;
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.title("Reports & Data Export"), BorderLayout.WEST);
        header.add(Theme.muted("Generate comprehensive business reports and export spreadsheet-compatible CSV files."),
                BorderLayout.SOUTH);

        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        left.setOpaque(false);
        reportSelector.setPreferredSize(new Dimension(280, 28));
        populateReports();
        left.add(new JLabel("Report:"));
        left.add(reportSelector);
        left.add(previewButton);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        right.add(exportButton);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        JPanel tableCard = new JPanel(new BorderLayout(6, 6));
        Theme.card(tableCard);
        statusLabel.setFont(Theme.SMALL);
        statusLabel.setForeground(Theme.MUTED);
        tableCard.add(statusLabel, BorderLayout.NORTH);
        tableCard.add(UiSupport.scroll(table), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(tableCard, BorderLayout.CENTER);

        previewButton.addActionListener(e -> generateReport());
        reportSelector.addActionListener(e -> generateReport());
        exportButton.addActionListener(e -> exportCsv());
    }

    private void populateReports() {
        reportSelector.addItem(new ReportOption("Inventory Valuation Report", "valuation"));
        reportSelector.addItem(new ReportOption("Full Inventory Catalog", "inventory"));
        reportSelector.addItem(new ReportOption("Out of Stock Products", "out_of_stock"));
        reportSelector.addItem(new ReportOption("Critical Stock Products", "critical_stock"));
        reportSelector.addItem(new ReportOption("Low Stock Products", "low_stock"));
        reportSelector.addItem(new ReportOption("Stock Movement Audit Log", "movements"));
        reportSelector.addItem(new ReportOption("High Demand Analysis Report", "demand"));
        reportSelector.addItem(new ReportOption("Product Expiry Tracking", "expiry"));
        reportSelector.addItem(new ReportOption("Supplier Directory", "suppliers"));
        reportSelector.addItem(new ReportOption("Purchase Order Summary", "orders"));
    }

    @Override
    public String id() {
        return "reports";
    }

    @Override
    public String title() {
        return "Reports";
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void refresh() {
        generateReport();
    }

    private void generateReport() {
        ReportOption opt = (ReportOption) reportSelector.getSelectedItem();
        if (opt == null) return;

        statusLabel.setText("Generating " + opt.name + "…");
        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                var svc = AppContext.get().reports();
                return switch (opt.key) {
                    case "valuation" -> svc.valuationReport();
                    case "inventory" -> svc.inventoryReport();
                    case "out_of_stock" -> svc.stockStatusReport(StockStatus.OUT_OF_STOCK);
                    case "critical_stock" -> svc.stockStatusReport(StockStatus.CRITICAL);
                    case "low_stock" -> svc.stockStatusReport(StockStatus.LOW);
                    case "movements" -> svc.movementReport();
                    case "demand" -> svc.highDemandReport();
                    case "expiry" -> svc.expiryReport();
                    case "suppliers" -> svc.supplierReport();
                    case "orders" -> svc.purchaseOrderReport();
                    default -> List.of();
                };
            }

            @Override
            protected void done() {
                try {
                    currentData = get();
                    bindTable(currentData);
                    statusLabel.setText("Report: " + opt.name + " · " + Math.max(0, currentData.size() - 1)
                            + " data rows · Generated at " + Dates.now());
                } catch (Exception ex) {
                    statusLabel.setText("Error generating report: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void bindTable(List<String[]> data) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        if (data == null || data.isEmpty()) return;

        String[] headers = data.get(0);
        for (String h : headers) {
            tableModel.addColumn(h);
        }

        for (int i = 1; i < data.size(); i++) {
            tableModel.addRow(data.get(i));
        }
    }

    private void exportCsv() {
        if (currentData.isEmpty()) {
            UiSupport.error(root, "Generate a report first before exporting.");
            return;
        }

        ReportOption opt = (ReportOption) reportSelector.getSelectedItem();
        String defaultName = (opt != null ? opt.key : "report") + "-" + Dates.now().replaceAll("[: ]", "-") + ".csv";

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        int ret = chooser.showSaveDialog(frame);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
                selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".csv");
            }
            Path path = selectedFile.toPath();
            try {
                AppContext.get().reports().exportCsv(opt != null ? opt.name : "Report", currentData, path);
                UiSupport.info(root, "Report successfully exported to:\n" + path.toAbsolutePath());
            } catch (Exception ex) {
                UiSupport.error(root, "Export failed: " + ex.getMessage());
            }
        }
    }

    private static class ReportOption {
        final String name;
        final String key;

        ReportOption(String name, String key) {
            this.name = name;
            this.key = key;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
