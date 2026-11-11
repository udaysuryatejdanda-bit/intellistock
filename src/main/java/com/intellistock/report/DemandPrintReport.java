package com.intellistock.report;

import com.intellistock.analytics.DemandAnalytics.ProductDemand;
import com.intellistock.util.Money;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JOptionPane;

/**
 * Renders a single-product demand analysis report using the built-in Java print API.
 * No external libraries required. On Windows 10/11 the system print dialog includes
 * "Microsoft Print to PDF" so users can save a PDF file directly.
 */
public final class DemandPrintReport implements Printable {

    // ── colour palette (mirrors Theme) ──────────────────────────────────────
    private static final Color HEADER_BG    = new Color(0x17233B);
    private static final Color ACCENT       = new Color(0x1E88E5);
    private static final Color DANGER       = new Color(0xC62828);
    private static final Color SUCCESS      = new Color(0x2E7D32);
    private static final Color WARNING      = new Color(0xF9A825);
    private static final Color TEXT         = new Color(0x1F2933);
    private static final Color MUTED        = new Color(0x6B7280);
    private static final Color LINE         = new Color(0xE5E7EB);
    private static final Color CARD_BG      = new Color(0xF3F5F8);

    // ── fonts ────────────────────────────────────────────────────────────────
    private static final Font FONT_HEADER   = new Font("Segoe UI", Font.BOLD,  18);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 10);
    private static final Font FONT_SECTION  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_LABEL    = new Font("Segoe UI", Font.BOLD,  11);
    private static final Font FONT_VALUE    = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_NOTE     = new Font("Segoe UI", Font.ITALIC, 10);
    private static final Font FONT_FOOTER   = new Font("Segoe UI", Font.PLAIN,  9);

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    private final ProductDemand demand;
    private final String generatedAt;

    public DemandPrintReport(ProductDemand demand) {
        if (demand == null) throw new IllegalArgumentException("demand must not be null");
        this.demand = demand;
        this.generatedAt = LocalDateTime.now().format(DISPLAY_FMT);
    }

    // ── public entry point ───────────────────────────────────────────────────

    /**
     * Opens the system print dialog. Call from the EDT; the print job runs in a
     * background thread to avoid freezing the UI.
     */
    public static void printForProduct(ProductDemand demand) {
        if (demand == null) {
            JOptionPane.showMessageDialog(null,
                    "Please select a product row in the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Demand Report – " + demand.product.getName());
        job.setPrintable(new DemandPrintReport(demand));

        // Show print dialog on the calling thread (already EDT-safe via SwingWorker in caller)
        if (!job.printDialog()) {
            return; // user cancelled
        }

        // Print on a background thread so the EDT stays responsive
        Thread printThread = new Thread(() -> {
            try {
                job.print();
            } catch (PrinterException ex) {
                javax.swing.SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null,
                                "Printing failed: " + ex.getMessage(),
                                "Print Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "demand-print");
        printThread.setDaemon(true);
        printThread.start();
    }

    // ── Printable implementation ─────────────────────────────────────────────

    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Translate to printable area origin
        g2.translate(pf.getImageableX(), pf.getImageableY());

        float w = (float) pf.getImageableWidth();
        float cursor = 0;

        // ── Header band ──────────────────────────────────────────────────────
        float headerH = 54;
        g2.setColor(HEADER_BG);
        g2.fillRect(0, (int) cursor, (int) w, (int) headerH);

        g2.setColor(Color.WHITE);
        g2.setFont(FONT_HEADER);
        drawString(g2, "IntelliStock", 14, cursor + 22);
        g2.setFont(FONT_SUBTITLE);
        g2.setColor(new Color(0xB0BEC5));
        drawString(g2, "Demand Analysis Report", 14, cursor + 38);
        g2.setColor(new Color(0xB0BEC5));
        drawStringRight(g2, "Generated: " + generatedAt, w - 14, cursor + 38);

        cursor += headerH + 16;

        // ── Product identity card ────────────────────────────────────────────
        cursor = drawSectionCard(g2, w, cursor,
                "PRODUCT SUMMARY",
                new String[][]{
                        {"Product Name",  demand.product.getName()},
                        {"SKU / Code",    demand.product.getSku()},
                        {"Category",      demand.product.getCategoryName()},
                        {"Unit",          demand.product.getUnit()},
                        {"Current Stock", demand.product.getQuantity() + " " + demand.product.getUnit()},
                        {"Cost Price",    Money.format(demand.product.getCostPriceCents())},
                        {"Selling Price", Money.format(demand.product.getSellingPriceCents())},
                        {"Location",      nvl(demand.product.getWarehouseLocation())},
                });

        cursor += 12;

        // ── Demand metrics card ──────────────────────────────────────────────
        String daysLeft = demand.daysRemaining != null
                ? String.format("%.1f days", demand.daysRemaining)
                : "N/A (no recent movement)";
        String reorder = (demand.suggestedReorder != null && demand.suggestedReorder > 0)
                ? String.valueOf(demand.suggestedReorder) + " " + demand.product.getUnit()
                : "Not required";

        cursor = drawSectionCard(g2, w, cursor,
                "DEMAND ANALYSIS METRICS",
                new String[][]{
                        {"Analysis Velocity",    demand.velocity},
                        {"High Demand Flag",     demand.highDemand ? "YES – unusually elevated" : "No"},
                        {"Recent Outbound",      demand.recentOutbound + " units (last window)"},
                        {"Baseline Outbound",    demand.previousOutbound + " units (prior window)"},
                        {"Recent Daily Average", String.format("%.2f units/day", demand.recentDailyAverage)},
                        {"Baseline Daily Avg",   String.format("%.2f units/day", demand.baselineDailyAverage)},
                        {"Estimated Days Left",  daysLeft},
                        {"Suggested Reorder",    reorder},
                });

        cursor += 12;

        // ── Analysis explanation ─────────────────────────────────────────────
        cursor = drawExplanationCard(g2, w, cursor, demand.explanation);

        cursor += 12;

        // ── Velocity badge ───────────────────────────────────────────────────
        cursor = drawVelocityBadge(g2, w, cursor);

        cursor += 12;

        // ── Footer ───────────────────────────────────────────────────────────
        g2.setFont(FONT_FOOTER);
        g2.setColor(MUTED);
        drawString(g2, "IntelliStock  |  Demand Analysis and Inventory Run-Rate Report", 0, cursor + 10);

        return PAGE_EXISTS;
    }

    // ── drawing helpers ──────────────────────────────────────────────────────

    /** Draws a labelled two-column card; returns the new Y cursor below the card. */
    private float drawSectionCard(Graphics2D g2, float w, float top,
                                   String sectionTitle, String[][] rows) {
        int rowH  = 19;
        int padX  = 12;
        int padY  = 10;
        float cardH = padY * 2 + 18 + rows.length * rowH + 4;

        // Card background
        g2.setColor(CARD_BG);
        g2.fillRoundRect(0, (int) top, (int) w, (int) cardH, 8, 8);
        g2.setColor(LINE);
        g2.setStroke(new BasicStroke(0.5f));
        g2.drawRoundRect(0, (int) top, (int) w - 1, (int) cardH - 1, 8, 8);

        // Section title
        float y = top + padY + 12;
        g2.setFont(FONT_SECTION);
        g2.setColor(ACCENT);
        drawString(g2, sectionTitle, padX, y);

        // Separator line
        y += 5;
        g2.setColor(LINE);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(padX, (int) y, (int) w - padX, (int) y);
        y += 6;

        // Rows
        float colW = w / 2f;
        for (String[] row : rows) {
            g2.setFont(FONT_LABEL);
            g2.setColor(MUTED);
            drawString(g2, row[0], padX, y + rowH - 4);
            g2.setFont(FONT_VALUE);
            g2.setColor(TEXT);
            drawString(g2, row[1], (int) colW, y + rowH - 4);
            y += rowH;
        }

        return top + cardH;
    }

    /** Draws the analysis explanation with word-wrap. */
    private float drawExplanationCard(Graphics2D g2, float w, float top, String text) {
        int padX  = 12;
        int padY  = 10;

        // Word-wrap the explanation to fit within the card
        java.util.List<String> lines = wordWrap(g2, FONT_NOTE, text, (int) w - padX * 2);
        float cardH = padY * 2 + 18 + lines.size() * 14 + 4;

        g2.setColor(new Color(0xFFFDE7));
        g2.fillRoundRect(0, (int) top, (int) w, (int) cardH, 8, 8);
        g2.setColor(new Color(0xF9A825, false));
        g2.setColor(WARNING);
        g2.setStroke(new BasicStroke(0.5f));
        g2.drawRoundRect(0, (int) top, (int) w - 1, (int) cardH - 1, 8, 8);

        float y = top + padY + 12;
        g2.setFont(FONT_SECTION);
        g2.setColor(new Color(0xE65100));
        drawString(g2, "ANALYSIS EXPLANATION", padX, y);

        y += 5;
        g2.setColor(new Color(0xFFE082));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(padX, (int) y, (int) w - padX, (int) y);
        y += 6;

        g2.setFont(FONT_NOTE);
        g2.setColor(TEXT);
        for (String line : lines) {
            y += 14;
            drawString(g2, line, padX, y);
        }

        return top + cardH;
    }

    /** Draws a coloured velocity status badge. */
    private float drawVelocityBadge(Graphics2D g2, float w, float top) {
        Color badgeColor = switch (demand.velocity) {
            case "FAST"    -> DANGER;
            case "MEDIUM"  -> WARNING;
            case "SLOW"    -> SUCCESS;
            default        -> MUTED;
        };
        String badgeText = "Velocity: " + demand.velocity
                + (demand.highDemand ? "  •  HIGH DEMAND FLAGGED" : "");

        g2.setColor(badgeColor);
        g2.fillRoundRect(0, (int) top, (int) w, 28, 8, 8);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(Color.WHITE);
        drawString(g2, badgeText, 14, top + 19);

        return top + 28;
    }

    // ── text utilities ───────────────────────────────────────────────────────

    private void drawString(Graphics2D g2, String text, float x, float y) {
        if (text == null || text.isBlank()) return;
        g2.drawString(text, x, y);
    }

    private void drawStringRight(Graphics2D g2, String text, float rightX, float y) {
        if (text == null || text.isBlank()) return;
        int sw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, rightX - sw, y);
    }

    private java.util.List<String> wordWrap(Graphics2D g2, Font font, String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        g2.setFont(font);
        java.awt.FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
