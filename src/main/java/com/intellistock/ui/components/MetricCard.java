package com.intellistock.ui.components;

import com.intellistock.ui.Theme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;

public class MetricCard extends JPanel {
    private final JLabel valueLabel = new JLabel("0");

    public MetricCard(String title, Color accent) {
        setLayout(new BorderLayout(4, 4));
        Theme.card(this);
        setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                javax.swing.BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel titleLabel = Theme.muted(title);
        valueLabel.setFont(Theme.TITLE);
        valueLabel.setForeground(Theme.TEXT);
        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}
