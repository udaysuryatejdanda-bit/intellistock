package com.intellistock.ui.components;

import com.intellistock.model.AlertSeverity;
import com.intellistock.model.OrderStatus;
import com.intellistock.model.StockStatus;
import com.intellistock.ui.Theme;

import javax.swing.JLabel;
import java.awt.Color;

public final class StatusBadge {
    private StatusBadge() {
    }

    public static JLabel of(StockStatus status) {
        return switch (status) {
            case NORMAL -> badge("Normal", Theme.SUCCESS);
            case LOW -> badge("Low stock", Theme.WARNING);
            case CRITICAL -> badge("Critical", Theme.DANGER);
            case OUT_OF_STOCK -> badge("Out of stock", new Color(0x4B5563));
            case ARCHIVED -> badge("Archived", Theme.MUTED);
        };
    }

    public static JLabel of(OrderStatus status) {
        return switch (status) {
            case DRAFT -> badge("Draft", Theme.MUTED);
            case PENDING -> badge("Pending", Theme.WARNING);
            case PARTIALLY_RECEIVED -> badge("Partial", Theme.INFO);
            case RECEIVED -> badge("Received", Theme.SUCCESS);
            case CANCELLED -> badge("Cancelled", Theme.DANGER);
        };
    }

    public static JLabel of(AlertSeverity severity) {
        return switch (severity) {
            case INFO -> badge("Info", Theme.INFO);
            case WARNING -> badge("Warning", Theme.WARNING);
            case CRITICAL -> badge("Critical", Theme.DANGER);
        };
    }

    public static JLabel badge(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(color);
        label.setForeground(Color.WHITE);
        label.setFont(Theme.SMALL);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return label;
    }
}
