package com.intellistock.ui;

import com.intellistock.config.AppContext;
import com.intellistock.model.User;
import com.intellistock.ui.pages.AlertsPage;
import com.intellistock.ui.pages.DashboardPage;
import com.intellistock.ui.pages.DemandPage;
import com.intellistock.ui.pages.ProductsPage;
import com.intellistock.ui.pages.PurchaseOrdersPage;
import com.intellistock.ui.pages.RegisterProductPage;
import com.intellistock.ui.pages.ReportsPage;
import com.intellistock.ui.pages.SettingsPage;
import com.intellistock.ui.pages.StockMovementsPage;
import com.intellistock.ui.pages.SuppliersPage;
import com.intellistock.ui.pages.UsersPage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private String current = "dashboard";

    public MainFrame() {
        super("IntelliStock AI");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        registerPages();
        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar(), BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        showPage("dashboard");
    }

    public static void open() {
        new MainFrame().setVisible(true);
    }

    public void showPage(String id) {
        current = id;
        cards.show(content, id);
        pages.get(id).refresh();
        navButtons.forEach((key, button) -> {
            boolean active = key.equals(id);
            button.setBackground(active ? Theme.SIDEBAR_ACTIVE : Theme.SIDEBAR);
            button.setForeground(Color.WHITE);
        });
    }

    public void refreshAll() {
        pages.values().forEach(Page::refresh);
        showPage(current);
    }

    private void registerPages() {
        addPage(new DashboardPage(this));
        addPage(new RegisterProductPage(this));
        addPage(new ProductsPage(this));
        addPage(new StockMovementsPage(this));
        addPage(new AlertsPage(this));
        addPage(new SuppliersPage(this));
        addPage(new PurchaseOrdersPage(this));
        addPage(new DemandPage(this));
        addPage(new ReportsPage(this));
        addPage(new UsersPage(this));
        addPage(new SettingsPage(this));
    }

    private void addPage(Page page) {
        pages.put(page.id(), page);
        content.add(page.component(), page.id());
    }

    private JPanel sidebar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.SIDEBAR);
        panel.setPreferredSize(new Dimension(230, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
        JLabel brand = new JLabel("IntelliStock AI");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brand.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(brand);
        User user = AppContext.get().auth().currentUser();
        JLabel who = new JLabel(user.getFullName() + " · " + user.getRole());
        who.setForeground(new Color(0x93C5FD));
        who.setFont(Theme.SMALL);
        who.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(who);
        panel.add(Box.createVerticalStrut(16));
        pages.forEach((id, page) -> {
            if ("users".equals(id) && user.getRole() != com.intellistock.model.Role.ADMIN) {
                return;
            }
            JButton button = nav(page.title(), id);
            navButtons.put(id, button);
            panel.add(button);
            panel.add(Box.createVerticalStrut(4));
        });
        panel.add(Box.createVerticalGlue());
        JButton logout = nav("Logout", "logout");
        logout.addActionListener(e -> {
            AppContext.get().auth().logout();
            dispose();
            LoginFrame.showLogin();
        });
        panel.add(logout);
        return panel;
    }

    private JButton nav(String title, String id) {
        JButton button = new JButton(title);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        button.setBackground(Theme.SIDEBAR);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (!"logout".equals(id)) {
            button.addActionListener(e -> showPage(id));
        }
        return button;
    }
}
