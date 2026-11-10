package com.intellistock.ui;

import javax.swing.JComponent;

public interface Page {
    String id();

    String title();

    JComponent component();

    void refresh();
}
