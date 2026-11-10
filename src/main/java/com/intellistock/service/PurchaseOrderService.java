package com.intellistock.service;

import com.intellistock.alerts.AlertEngine;
import com.intellistock.config.Database;
import com.intellistock.model.MovementType;
import com.intellistock.model.OrderStatus;
import com.intellistock.model.PurchaseOrder;
import com.intellistock.model.PurchaseOrderItem;
import com.intellistock.model.User;
import com.intellistock.repository.AuditRepository;
import com.intellistock.repository.PurchaseOrderRepository;
import com.intellistock.security.Permissions;
import com.intellistock.util.Dates;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PurchaseOrderService {
    private final Database database;
    private final PurchaseOrderRepository orders = new PurchaseOrderRepository();
    private final AuditRepository audit = new AuditRepository();
    private final InventoryService inventory;
    private final AlertEngine alertEngine;

    public PurchaseOrderService(Database database, InventoryService inventory, AlertEngine alertEngine) {
        this.database = database;
        this.inventory = inventory;
        this.alertEngine = alertEngine;
    }

    public PurchaseOrder saveDraft(User user, PurchaseOrder order) {
        Permissions.requireUser(user);
        if (order.getSupplierId() <= 0) {
            throw new IllegalArgumentException("Supplier is required");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Add at least one product line");
        }
        order.setCreatedBy(user.getId());
        order.setCreatedAt(Dates.now());
        order.setStatus(OrderStatus.DRAFT);
        return database.inTransaction(connection -> {
            long id = orders.insertOrder(connection, order);
            order.setId(id);
            for (PurchaseOrderItem item : order.getItems()) {
                if (item.getQuantityOrdered() <= 0) {
                    throw new IllegalArgumentException("Ordered quantity must be greater than zero");
                }
                item.setPurchaseOrderId(id);
                item.setQuantityReceived(0);
                orders.insertItem(connection, item);
            }
            audit.insert(connection, user.getId(), "PO_DRAFT", "PURCHASE_ORDER", id, "Draft saved");
            return orders.findById(connection, id).orElse(order);
        });
    }

    public PurchaseOrder submit(User user, long orderId) {
        Permissions.requireUser(user);
        PurchaseOrder updated = database.inTransaction(connection -> {
            PurchaseOrder order = orders.findById(connection, orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase order was not found"));
            if (order.getStatus() != OrderStatus.DRAFT) {
                throw new IllegalArgumentException("Only draft orders can be submitted");
            }
            String now = Dates.now();
            orders.updateStatus(connection, orderId, OrderStatus.PENDING, now, null);
            audit.insert(connection, user.getId(), "PO_SUBMIT", "PURCHASE_ORDER", orderId, "Submitted");
            return orders.findById(connection, orderId).orElse(order);
        });
        alertEngine.refresh();
        return updated;
    }

    public PurchaseOrder cancel(User user, long orderId) {
        Permissions.requireUser(user);
        PurchaseOrder updated = database.inTransaction(connection -> {
            PurchaseOrder order = orders.findById(connection, orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase order was not found"));
            if (order.getStatus() == OrderStatus.RECEIVED) {
                throw new IllegalArgumentException("Received orders cannot be cancelled");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                return order;
            }
            if (order.getItems().stream().anyMatch(item -> item.getQuantityReceived() > 0)
                    && order.getStatus() != OrderStatus.DRAFT) {
                throw new IllegalArgumentException("Orders with receipts cannot be cancelled");
            }
            orders.updateStatus(connection, orderId, OrderStatus.CANCELLED, null, Dates.now());
            audit.insert(connection, user.getId(), "PO_CANCEL", "PURCHASE_ORDER", orderId, "Cancelled");
            return orders.findById(connection, orderId).orElse(order);
        });
        alertEngine.refresh();
        return updated;
    }

    public PurchaseOrder receive(User user, long orderId, Map<Long, Integer> productQuantities, String receiptKey) {
        Permissions.requireUser(user);
        if (receiptKey == null || receiptKey.isBlank()) {
            receiptKey = UUID.randomUUID().toString();
        }
        String key = receiptKey;
        PurchaseOrder updated = database.inTransaction(connection -> {
            if (orders.receiptKeyExists(connection, key)) {
                throw new IllegalArgumentException("This receipt was already processed");
            }
            PurchaseOrder order = orders.findById(connection, orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Purchase order was not found"));
            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PARTIALLY_RECEIVED) {
                throw new IllegalArgumentException("Only pending orders can be received");
            }
            long receiptId = orders.insertReceipt(connection, orderId, user.getId(), Dates.now(), "Goods receipt", key);
            boolean any = false;
            for (PurchaseOrderItem item : order.getItems()) {
                int qty = productQuantities.getOrDefault(item.getProductId(), 0);
                if (qty < 0) {
                    throw new IllegalArgumentException("Received quantity cannot be negative");
                }
                if (qty == 0) {
                    continue;
                }
                if (qty > item.outstanding()) {
                    throw new IllegalArgumentException(
                            "Cannot receive more than outstanding quantity for " + item.getSku());
                }
                any = true;
                orders.addReceived(connection, item.getId(), qty);
                orders.insertReceiptItem(connection, receiptId, item.getProductId(), qty);
                inventory.apply(connection, user, item.getProductId(), MovementType.PO_RECEIPT, qty, true,
                        "Purchase order receipt", "PO-" + orderId, orderId, receiptId);
            }
            if (!any) {
                throw new IllegalArgumentException("Enter at least one received quantity");
            }
            PurchaseOrder refreshed = orders.findById(connection, orderId).orElse(order);
            boolean complete = refreshed.getItems().stream().allMatch(item -> item.outstanding() == 0);
            boolean partial = refreshed.getItems().stream().anyMatch(item -> item.getQuantityReceived() > 0);
            OrderStatus next = complete ? OrderStatus.RECEIVED : (partial ? OrderStatus.PARTIALLY_RECEIVED : OrderStatus.PENDING);
            orders.updateStatus(connection, orderId, next, null, null);
            audit.insert(connection, user.getId(), "PO_RECEIPT", "PURCHASE_ORDER", orderId, "key=" + key);
            return orders.findById(connection, orderId).orElse(refreshed);
        });
        alertEngine.refresh();
        return updated;
    }

    public List<PurchaseOrder> list() {
        return database.inTransaction(orders::findAll);
    }

    public PurchaseOrder get(long id) {
        return database.inTransaction(connection -> orders.findById(connection, id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order was not found")));
    }

    public List<PurchaseOrder> bySupplier(long supplierId) {
        return database.inTransaction(connection -> orders.findBySupplier(connection, supplierId));
    }
}
