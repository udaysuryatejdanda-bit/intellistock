package com.intellistock.config;

import com.intellistock.alerts.AlertEngine;
import com.intellistock.analytics.DemandAnalytics;
import com.intellistock.service.AuthService;
import com.intellistock.service.BackupService;
import com.intellistock.service.DashboardService;
import com.intellistock.service.InventoryService;
import com.intellistock.service.ProductService;
import com.intellistock.service.PurchaseOrderService;
import com.intellistock.service.ReportService;
import com.intellistock.service.SupplierService;
import com.intellistock.service.UserService;
import com.intellistock.util.AppPaths;

import java.nio.file.Files;

public final class AppContext {
    private static AppContext instance;

    private final Database database;
    private final AuthService authService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final PurchaseOrderService purchaseOrderService;
    private final UserService userService;
    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final BackupService backupService;
    private final AlertEngine alertEngine;
    private final DemandAnalytics demandAnalytics;

    private AppContext(Database database) {
        this.database = database;
        this.alertEngine = new AlertEngine(database);
        this.demandAnalytics = new DemandAnalytics(database);
        this.alertEngine.setDemandAnalytics(demandAnalytics);
        this.authService = new AuthService(database);
        this.inventoryService = new InventoryService(database, alertEngine);
        this.productService = new ProductService(database, inventoryService, alertEngine);
        this.supplierService = new SupplierService(database);
        this.purchaseOrderService = new PurchaseOrderService(database, inventoryService, alertEngine);
        this.userService = new UserService(database);
        this.dashboardService = new DashboardService(database, alertEngine, demandAnalytics);
        this.reportService = new ReportService(database, demandAnalytics);
        this.backupService = new BackupService(database);
    }

    public static synchronized AppContext initialize(boolean resetDemo) {
        if (resetDemo) {
            try {
                Files.deleteIfExists(AppPaths.databaseFile());
            } catch (Exception ignored) {
                throw new IllegalStateException("Unable to reset the demonstration database");
            }
        }
        Database database = Database.applicationDatabase();
        database.initializeSchema();
        instance = new AppContext(database);
        new DemoDataSeeder(database).seedIfNeeded(instance.inventoryService, instance.alertEngine);
        instance.alertEngine.refresh();
        return instance;
    }

    public static AppContext get() {
        if (instance == null) {
            throw new IllegalStateException("Application context is not ready");
        }
        return instance;
    }

    public static AppContext forTesting(Database database) {
        AppContext context = new AppContext(database);
        database.initializeSchema();
        instance = context;
        return context;
    }

    public Database database() {
        return database;
    }

    public AuthService auth() {
        return authService;
    }

    public ProductService products() {
        return productService;
    }

    public InventoryService inventory() {
        return inventoryService;
    }

    public SupplierService suppliers() {
        return supplierService;
    }

    public PurchaseOrderService purchaseOrders() {
        return purchaseOrderService;
    }

    public UserService users() {
        return userService;
    }

    public DashboardService dashboard() {
        return dashboardService;
    }

    public ReportService reports() {
        return reportService;
    }

    public BackupService backups() {
        return backupService;
    }

    public AlertEngine alerts() {
        return alertEngine;
    }

    public DemandAnalytics demand() {
        return demandAnalytics;
    }
}
