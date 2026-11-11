package com.intellistock.alerts;

import com.intellistock.analytics.DemandAnalytics;
import com.intellistock.config.Database;
import com.intellistock.model.AlertSeverity;
import com.intellistock.model.AlertStatus;
import com.intellistock.model.AlertType;
import com.intellistock.model.InventoryAlert;
import com.intellistock.model.OrderStatus;
import com.intellistock.model.Product;
import com.intellistock.model.PurchaseOrder;
import com.intellistock.model.StockStatus;
import com.intellistock.repository.AlertRepository;
import com.intellistock.repository.ProductRepository;
import com.intellistock.repository.PurchaseOrderRepository;
import com.intellistock.repository.SettingsRepository;
import com.intellistock.util.Dates;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Rule-based inventory alert engine. Conditions are recalculated from stored records.
 * Duplicate active alerts for the same product and type are reused rather than inserted again.
 */
public class AlertEngine {
    private final Database database;
    private final ProductRepository products = new ProductRepository();
    private final AlertRepository alerts = new AlertRepository();
    private final PurchaseOrderRepository purchaseOrders = new PurchaseOrderRepository();
    private final SettingsRepository settings = new SettingsRepository();
    private DemandAnalytics demandAnalytics;

    public AlertEngine(Database database) {
        this.database = database;
    }

    public void setDemandAnalytics(DemandAnalytics demandAnalytics) {
        this.demandAnalytics = demandAnalytics;
    }

    public void refresh() {
        database.execute(this::refresh);
    }

    public void refresh(Connection connection) throws Exception {
        int expiryDays = Integer.parseInt(settings.get(connection, "alerts.expiry.days").orElse("30"));
        List<Product> catalog = products.findAll(connection, false);
        for (Product product : catalog) {
            StockStatus status = product.getStockStatus();
            upsertStockAlert(connection, product, AlertType.OUT_OF_STOCK, status == StockStatus.OUT_OF_STOCK,
                    AlertSeverity.CRITICAL, product.getName() + " is out of stock",
                    "Available quantity is 0.");
            upsertStockAlert(connection, product, AlertType.CRITICAL_STOCK, status == StockStatus.CRITICAL,
                    AlertSeverity.CRITICAL,
                    product.getName() + " is at critical stock (" + product.getQuantity() + ")",
                    "Quantity is at or below the critical threshold of " + product.getCriticalThreshold() + ".");
            boolean low = status == StockStatus.LOW;
            upsertStockAlert(connection, product, AlertType.LOW_STOCK, low, AlertSeverity.WARNING,
                    product.getName() + " is low on stock (" + product.getQuantity() + ")",
                    "Quantity is at or below the reorder threshold of " + product.getReorderThreshold() + ".");

            Optional<LocalDate> expiry = Dates.parseDate(product.getExpiryDate());
            boolean expired = expiry.isPresent() && expiry.get().isBefore(LocalDate.now());
            boolean expiring = expiry.isPresent() && !expired
                    && !expiry.get().isAfter(LocalDate.now().plusDays(expiryDays));
            upsertStockAlert(connection, product, AlertType.EXPIRED, expired, AlertSeverity.CRITICAL,
                    product.getName() + " has expired",
                    "Expiry date " + product.getExpiryDate() + " is in the past.");
            upsertStockAlert(connection, product, AlertType.EXPIRING, expiring, AlertSeverity.WARNING,
                    product.getName() + " expires on " + product.getExpiryDate(),
                    "Expiry is within the configured " + expiryDays + "-day warning period.");
        }

        if (demandAnalytics != null) {
            for (DemandAnalytics.ProductDemand demand : demandAnalytics.analyze(connection)) {
                upsertStockAlert(connection, demand.product, AlertType.HIGH_DEMAND, demand.highDemand,
                        AlertSeverity.WARNING,
                        demand.product.getName() + " has unusually high recent demand",
                        "Recent daily average " + round(demand.recentDailyAverage)
                                + " compared with baseline " + round(demand.baselineDailyAverage)
                                + ". This is a moving-average comparison, not a machine-learning forecast.");
                boolean recommend = demand.suggestedReorder != null && demand.suggestedReorder > 0
                        && (demand.product.getStockStatus() == StockStatus.LOW
                        || demand.product.getStockStatus() == StockStatus.CRITICAL
                        || demand.product.getStockStatus() == StockStatus.OUT_OF_STOCK);
                upsertStockAlert(connection, demand.product, AlertType.REORDER, recommend, AlertSeverity.INFO,
                        "Reorder " + (demand.suggestedReorder == null ? 0 : demand.suggestedReorder)
                                + " of " + demand.product.getName(),
                        demand.explanation);
            }
        }

        List<PurchaseOrder> pending = purchaseOrders.findByStatus(connection, OrderStatus.PENDING);
        pending.addAll(purchaseOrders.findByStatus(connection, OrderStatus.PARTIALLY_RECEIVED));
        for (PurchaseOrder order : pending) {
            boolean delayed = false;
            Optional<LocalDate> expected = Dates.parseDate(order.getExpectedDeliveryDate());
            if (expected.isPresent() && expected.get().isBefore(LocalDate.now())) {
                delayed = true;
            }
            String message = delayed
                    ? "Purchase order #" + order.getId() + " is past its expected delivery date"
                    : (order.getStatus() == OrderStatus.PARTIALLY_RECEIVED
                        ? "Purchase order #" + order.getId() + " is partially received; remaining items pending"
                        : "Purchase order #" + order.getId() + " is awaiting receipt");
            upsertPoAlert(connection, order, true, delayed
                    ? AlertSeverity.WARNING : AlertSeverity.INFO, message);
        }
        for (PurchaseOrder order : purchaseOrders.findAll(connection)) {
            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PARTIALLY_RECEIVED) {
                resolvePo(connection, order.getId());
            }
        }
    }

    public List<InventoryAlert> openAlerts() {
        return database.inTransaction(alerts::findOpen);
    }

    public List<InventoryAlert> allAlerts() {
        return database.inTransaction(alerts::findAll);
    }

    public void acknowledge(long alertId) {
        database.execute(connection -> {
            InventoryAlert alert = alerts.findById(connection, alertId)
                    .orElseThrow(() -> new IllegalArgumentException("Alert was not found"));
            if (alert.getStatus() == AlertStatus.RESOLVED) {
                return;
            }
            alert.setStatus(AlertStatus.ACKNOWLEDGED);
            alert.setUpdatedAt(Dates.now());
            alerts.update(connection, alert);
        });
    }

    public void resolve(long alertId) {
        database.execute(connection -> {
            InventoryAlert alert = alerts.findById(connection, alertId)
                    .orElseThrow(() -> new IllegalArgumentException("Alert was not found"));
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setUpdatedAt(Dates.now());
            alert.setResolvedAt(Dates.now());
            alerts.update(connection, alert);
        });
    }

    private void upsertStockAlert(Connection connection, Product product, AlertType type, boolean condition,
                                  AlertSeverity severity, String message, String explanation) throws Exception {
        Optional<InventoryAlert> existing = alerts.findOpen(connection, product.getId(), null, type);
        if (condition) {
            if (existing.isPresent()) {
                InventoryAlert alert = existing.get();
                alert.setSeverity(severity);
                alert.setMessage(message);
                alert.setExplanation(explanation);
                alert.setUpdatedAt(Dates.now());
                alerts.update(connection, alert);
            } else {
                InventoryAlert alert = newAlert(product.getId(), null, type, severity, message, explanation);
                alerts.insert(connection, alert);
            }
        } else {
            existing.ifPresent(alert -> {
                try {
                    alert.setStatus(AlertStatus.RESOLVED);
                    alert.setUpdatedAt(Dates.now());
                    alert.setResolvedAt(Dates.now());
                    alerts.update(connection, alert);
                } catch (SQLRuntime ignored) {
                    throw ignored;
                } catch (Exception error) {
                    throw new SQLRuntime(error);
                }
            });
        }
    }

    private void upsertPoAlert(Connection connection, PurchaseOrder order, boolean condition, AlertSeverity severity,
                               String message) throws Exception {
        Optional<InventoryAlert> existing = alerts.findOpen(connection, null, order.getId(), AlertType.PENDING_PO);
        if (condition) {
            if (existing.isPresent()) {
                InventoryAlert alert = existing.get();
                alert.setSeverity(severity);
                alert.setMessage(message);
                alert.setUpdatedAt(Dates.now());
                alerts.update(connection, alert);
            } else {
                alerts.insert(connection, newAlert(null, order.getId(), AlertType.PENDING_PO, severity, message,
                        "Pending purchase-order monitoring uses recorded expected delivery dates."));
            }
        }
    }

    private void resolvePo(Connection connection, long orderId) throws Exception {
        Optional<InventoryAlert> existing = alerts.findOpen(connection, null, orderId, AlertType.PENDING_PO);
        if (existing.isPresent()) {
            InventoryAlert alert = existing.get();
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setUpdatedAt(Dates.now());
            alert.setResolvedAt(Dates.now());
            alerts.update(connection, alert);
        }
    }

    private InventoryAlert newAlert(Long productId, Long poId, AlertType type, AlertSeverity severity,
                                    String message, String explanation) {
        InventoryAlert alert = new InventoryAlert();
        alert.setProductId(productId);
        alert.setPurchaseOrderId(poId);
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setExplanation(explanation);
        alert.setStatus(AlertStatus.ACTIVE);
        String now = Dates.now();
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);
        return alert;
    }

    private static String round(double value) {
        return String.format("%.2f", value);
    }

    private static final class SQLRuntime extends RuntimeException {
        private SQLRuntime(Exception error) {
            super(error);
        }
    }
}
