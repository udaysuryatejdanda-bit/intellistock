package com.intellistock.service;

import com.intellistock.alerts.AlertEngine;
import com.intellistock.analytics.DemandAnalytics;
import com.intellistock.config.Database;
import com.intellistock.model.InventoryAlert;
import com.intellistock.model.OrderStatus;
import com.intellistock.model.Product;
import com.intellistock.model.PurchaseOrder;
import com.intellistock.model.StockMovement;
import com.intellistock.model.StockStatus;
import com.intellistock.repository.ProductRepository;
import com.intellistock.repository.PurchaseOrderRepository;
import com.intellistock.repository.StockMovementRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {
    private final Database database;
    private final ProductRepository products = new ProductRepository();
    private final StockMovementRepository movements = new StockMovementRepository();
    private final PurchaseOrderRepository purchaseOrders = new PurchaseOrderRepository();
    private final AlertEngine alertEngine;
    private final DemandAnalytics demandAnalytics;

    public DashboardService(Database database, AlertEngine alertEngine, DemandAnalytics demandAnalytics) {
        this.database = database;
        this.alertEngine = alertEngine;
        this.demandAnalytics = demandAnalytics;
    }

    public DashboardSnapshot snapshot() {
        return database.inTransaction(connection -> {
            List<Product> catalog = products.findAll(connection, false);
            DashboardSnapshot snapshot = new DashboardSnapshot();
            snapshot.totalProducts = catalog.size();
            for (Product product : catalog) {
                snapshot.totalQuantity += product.getQuantity();
                snapshot.valuationCents += product.getQuantity() * product.getCostPriceCents();
                StockStatus status = product.getStockStatus();
                if (status == StockStatus.LOW) {
                    snapshot.lowStock++;
                } else if (status == StockStatus.CRITICAL) {
                    snapshot.criticalStock++;
                } else if (status == StockStatus.OUT_OF_STOCK) {
                    snapshot.outOfStock++;
                }
                if (product.getExpiryDate() != null) {
                    var date = com.intellistock.util.Dates.parseDate(product.getExpiryDate());
                    if (date.isPresent() && !date.get().isAfter(LocalDate.now().plusDays(30))) {
                        snapshot.nearingExpiry++;
                    }
                }
            }
            snapshot.pendingOrders = purchaseOrders.findByStatus(connection, OrderStatus.PENDING).size()
                    + purchaseOrders.findByStatus(connection, OrderStatus.PARTIALLY_RECEIVED).size();
            snapshot.recentMovements = movements.findRecent(connection, 8);
            snapshot.openAlerts = alertEngine.openAlerts();
            List<DemandAnalytics.ProductDemand> demand = demandAnalytics.analyze(connection);
            for (DemandAnalytics.ProductDemand row : demand) {
                if (row.highDemand) {
                    snapshot.highDemand.add(row);
                }
                if (row.suggestedReorder != null && row.suggestedReorder > 0
                        && row.product.getStockStatus() != StockStatus.NORMAL) {
                    snapshot.reorderRecommendations.add(row);
                }
            }
            snapshot.movementTrend = movementTrend(connection);
            snapshot.demandTrend = demandTrend(demand);
            return snapshot;
        });
    }

    private Map<String, Integer> movementTrend(java.sql.Connection connection) throws Exception {
        Map<String, Integer> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            trend.put(today.minusDays(i).toString(), 0);
        }
        for (StockMovement movement : movements.findRecent(connection, 500)) {
            String day = movement.getOccurredAt().substring(0, 10);
            if (trend.containsKey(day)) {
                trend.put(day, trend.get(day) + Math.abs(movement.getQuantityDelta()));
            }
        }
        return trend;
    }

    private Map<String, Integer> demandTrend(List<DemandAnalytics.ProductDemand> demand) {
        Map<String, Integer> trend = new LinkedHashMap<>();
        demand.stream().limit(8).forEach(row -> trend.put(row.product.getSku(), row.recentOutbound));
        return trend;
    }

    public static final class DashboardSnapshot {
        public int totalProducts;
        public int totalQuantity;
        public long valuationCents;
        public int lowStock;
        public int outOfStock;
        public int criticalStock;
        public int pendingOrders;
        public int nearingExpiry;
        public List<StockMovement> recentMovements = new ArrayList<>();
        public List<InventoryAlert> openAlerts = new ArrayList<>();
        public List<DemandAnalytics.ProductDemand> highDemand = new ArrayList<>();
        public List<DemandAnalytics.ProductDemand> reorderRecommendations = new ArrayList<>();
        public Map<String, Integer> movementTrend = new LinkedHashMap<>();
        public Map<String, Integer> demandTrend = new LinkedHashMap<>();
        public List<PurchaseOrder> pendingOrderList = new ArrayList<>();
    }
}
