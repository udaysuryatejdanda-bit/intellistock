package com.intellistock.ui.components;

import com.intellistock.ui.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

public class BarChartPanel extends JPanel {
    private Map<String, Integer> data = new LinkedHashMap<>();
    private String emptyMessage = "No data yet";

    public BarChartPanel() {
        setPreferredSize(new Dimension(400, 220));
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    public void setData(Map<String, Integer> data, String emptyMessage) {
        this.data = data == null ? Map.of() : data;
        this.emptyMessage = emptyMessage;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (data.isEmpty() || data.values().stream().allMatch(v -> v == 0)) {
            g2.setColor(Theme.MUTED);
            g2.drawString(emptyMessage, 16, getHeight() / 2);
            g2.dispose();
            return;
        }
        int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int n = data.size();
        int pad = 32;
        int chartH = getHeight() - 50;
        int barW = Math.max(12, (getWidth() - pad * 2) / (n * 2));
        int i = 0;
        for (var entry : data.entrySet()) {
            int h = (int) ((entry.getValue() / (double) max) * (chartH - 10));
            int x = pad + i * (barW * 2);
            int y = chartH - h;
            g2.setColor(Theme.SIDEBAR_ACTIVE);
            g2.fillRoundRect(x, y, barW, h, 6, 6);
            g2.setColor(Theme.MUTED);
            g2.setFont(Theme.SMALL);
            FontMetrics fm = g2.getFontMetrics();
            String label = entry.getKey();
            if (label.length() > 8) {
                label = label.substring(Math.max(0, label.length() - 5));
            }
            g2.drawString(label, x, getHeight() - 12);
            g2.drawString(String.valueOf(entry.getValue()), x, y - 4);
            i++;
            fm.stringWidth(label);
        }
        g2.dispose();
    }
}
