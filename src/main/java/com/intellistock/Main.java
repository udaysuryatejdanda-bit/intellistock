
package com.intellistock;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.intellistock.config.AppContext;
import com.intellistock.ui.LoginFrame;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.err.println("[STARTUP] Main started");

        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception e) {
            System.err.println("[STARTUP] Look and feel failed: " + e);
        }

        boolean resetDemo = args != null &&
            java.util.Arrays.asList(args).contains("--reset-demo");

        SwingUtilities.invokeLater(() -> {
            System.err.println("[STARTUP] Event thread started");

            try {
                System.err.println("[STARTUP] Initializing database and services");
                AppContext.initialize(resetDemo);
                System.err.println("[STARTUP] AppContext initialized");

                LoginFrame frame = new LoginFrame();
                System.err.println("[STARTUP] LoginFrame constructed");

                frame.setVisible(true);
                System.err.println("[STARTUP] LoginFrame made visible");
                System.err.println("[STARTUP] Size: " +
                    frame.getSize() + ", showing: " + frame.isShowing());

            } catch (Throwable error) {
                error.printStackTrace();

                JOptionPane.showMessageDialog(
                    null,
                    error.toString(),
                    "IntelliStock AI startup error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
