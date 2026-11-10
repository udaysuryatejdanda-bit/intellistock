package com.intellistock.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

public final class Theme {
    public static final Color SIDEBAR = new Color(0x17233B);
    public static final Color SIDEBAR_ACTIVE = new Color(0x1E88E5);
    public static final Color SIDEBAR_HOVER = new Color(0x243656);
    public static final Color BACKGROUND = new Color(0xF3F5F8);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(0x1F2933);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color LINE = new Color(0xE5E7EB);
    public static final Color SUCCESS = new Color(0x2E7D32);
    public static final Color WARNING = new Color(0xF9A825);
    public static final Color DANGER = new Color(0xC62828);
    public static final Color INFO = new Color(0x1565C0);

    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    private Theme() {
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SMALL);
        label.setForeground(MUTED);
        return label;
    }

    public static JButton primary(String text) {
        JButton button = new JButton(text);
        button.setBackground(SIDEBAR_ACTIVE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        return button;
    }

    public static JButton secondary(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        return button;
    }

    public static void card(JComponent component) {
        component.setBackground(CARD);
        component.setOpaque(true);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
    }
}
