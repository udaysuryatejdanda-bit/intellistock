package com.intellistock.ui;

import com.intellistock.config.AppContext;
import com.intellistock.ui.components.UiSupport;
import com.intellistock.util.AppLogger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.logging.Level;

public class LoginFrame extends JFrame {
    private final JTextField username = UiSupport.field();
    private final JPasswordField password = new JPasswordField();
    private final JLabel error = new JLabel(" ");
    private final JButton login = Theme.primary("Sign in");

    public LoginFrame() {
        super("IntelliStock — Sign in");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(980, 620);
        setMinimumSize(new Dimension(860, 540));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.add(brandPanel(), BorderLayout.WEST);
        root.add(formPanel(), BorderLayout.CENTER);
        setContentPane(root);
        getRootPane().setDefaultButton(login);
        login.addActionListener(e -> doLogin());
    }

    public static void showLogin() {
        new LoginFrame().setVisible(true);
    }

    private JPanel brandPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.SIDEBAR);
        panel.setPreferredSize(new Dimension(380, 0));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(48, 36, 48, 36));
        JLabel title = new JLabel("<html><div style='color:white'><h1>IntelliStock</h1>"
                + "<p>Inventory and stock management for warehouses, supplies, and purchase orders.</p></div></html>");
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.NORTH);
        return panel;
    }

    private JPanel formPanel() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(Theme.BACKGROUND);
        JPanel card = new JPanel(new BorderLayout(8, 12));
        Theme.card(card);
        card.setPreferredSize(new Dimension(420, 340));
        card.add(Theme.title("Sign in"), BorderLayout.NORTH);
        JPanel fields = UiSupport.formGrid();
        error.setForeground(Theme.DANGER);
        password.setPreferredSize(new Dimension(240, 28));
        fields.add(UiSupport.labeled("Username", username, new JLabel(" ")), UiSupport.gbc(0, 0));
        fields.add(UiSupport.labeled("Password", password, new JLabel(" ")), UiSupport.gbc(0, 1));
        fields.add(error, UiSupport.gbc(0, 2));
        card.add(fields, BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(login, BorderLayout.EAST);
        actions.add(Theme.muted("Demo accounts are documented in README.md"), BorderLayout.WEST);
        card.add(actions, BorderLayout.SOUTH);
        wrap.add(card);
        return wrap;
    }

    private void doLogin() {
        login.setEnabled(false);
        error.setText("Signing in…");
        String user = username.getText().trim();
        char[] pass = password.getPassword();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                AppContext.get().auth().login(user, pass);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dispose();
                    MainFrame.open();
                } catch (Exception ex) {
                    AppLogger.get().log(Level.INFO, "Login failed");
                    error.setText(unwrap(ex));
                    login.setEnabled(true);
                }
            }
        }.execute();
    }

    private static String unwrap(Exception error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? "Unable to sign in" : message;
    }
}
