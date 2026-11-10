package com.intellistock.ui.components;

import com.intellistock.ui.Theme;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class UiSupport {
    private UiSupport() {
    }

    public static JTextField field() {
        JTextField field = new JTextField();
        field.setFont(Theme.BODY);
        field.setPreferredSize(new Dimension(240, 28));
        return field;
    }

    public static JPanel labeled(String label, JComponent field, JLabel error) {
        JPanel panel = new JPanel(new BorderLayout(4, 2));
        panel.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setFont(Theme.SMALL);
        error.setFont(Theme.SMALL);
        error.setForeground(Theme.DANGER);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.add(error, BorderLayout.SOUTH);
        return panel;
    }

    public static GridBagConstraints gbc(int x, int y) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(6, 6, 6, 6);
        return constraints;
    }

    public static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(Theme.BODY);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(new TableRowSorter<>(model));
        table.getTableHeader().setFont(Theme.SMALL);
        return table;
    }

    public static JScrollPane scroll(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(Theme.LINE));
        return scroll;
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "IntelliStock AI", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "IntelliStock AI", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static <T> T selected(JComboBox<T> combo) {
        return combo.getItemAt(combo.getSelectedIndex());
    }

    public static JPanel formGrid() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }
}
