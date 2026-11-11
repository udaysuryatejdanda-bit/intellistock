package com.intellistock.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

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

    public static final Color BTN_PRIMARY_BG = new Color(0x1565C0);
    public static final Color BTN_PRIMARY_HOVER = new Color(0x1976D2);
    public static final Color BTN_PRIMARY_PRESSED = new Color(0x0D47A1);
    public static final Color BTN_PRIMARY_BORDER = new Color(0x0D47A1);

    public static final Color BTN_SECONDARY_BG = new Color(0xE2E8F0);
    public static final Color BTN_SECONDARY_HOVER = new Color(0xCBD5E1);
    public static final Color BTN_SECONDARY_PRESSED = new Color(0x94A3B8);
    public static final Color BTN_SECONDARY_BORDER = new Color(0x64748B);
    public static final Color BTN_SECONDARY_TEXT = new Color(0x0F172A);

    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font BUTTON_PRIMARY_FONT = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font BUTTON_SECONDARY_FONT = new Font("Segoe UI", Font.BOLD, 12);

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
        return new StyledButton(
                text,
                BTN_PRIMARY_BG,
                BTN_PRIMARY_HOVER,
                BTN_PRIMARY_PRESSED,
                BTN_PRIMARY_BORDER,
                Color.WHITE,
                BUTTON_PRIMARY_FONT,
                7, 16);
    }

    public static JButton secondary(String text) {
        return new StyledButton(
                text,
                BTN_SECONDARY_BG,
                BTN_SECONDARY_HOVER,
                BTN_SECONDARY_PRESSED,
                BTN_SECONDARY_BORDER,
                BTN_SECONDARY_TEXT,
                BUTTON_SECONDARY_FONT,
                6, 14);
    }

    private static class StyledButton extends JButton {
        private final Color normalBg;
        private final Color hoverBg;
        private final Color pressedBg;
        private final Color borderColor;

        StyledButton(String text, Color normalBg, Color hoverBg, Color pressedBg,
                     Color borderColor, Color textColor, Font font, int padY, int padX) {
            super(text);
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            this.pressedBg = pressedBg;
            this.borderColor = borderColor;

            setFont(font);
            setForeground(textColor);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(padY, padX, padY, padX));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color bg;
            Color border;
            if (!isEnabled()) {
                bg = new Color(0xE2E8F0);
                border = new Color(0xCBD5E1);
            } else if (getModel().isPressed()) {
                bg = pressedBg;
                border = borderColor;
            } else if (getModel().isRollover()) {
                bg = hoverBg;
                border = borderColor;
            } else {
                bg = normalBg;
                border = borderColor;
            }

            int w = getWidth();
            int h = getHeight();

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w, h, 6, 6);

            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void card(JComponent component) {
        component.setBackground(CARD);
        component.setOpaque(true);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
    }
}
