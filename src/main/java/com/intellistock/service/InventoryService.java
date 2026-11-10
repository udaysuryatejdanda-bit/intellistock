package com.intellistock.service;

import com.intellistock.alerts.AlertEngine;
import com.intellistock.config.Database;
import com.intellistock.model.MovementType;
import com.intellistock.model.Product;
import com.intellistock.model.StockMovement;
import com.intellistock.model.User;
import com.intellistock.repository.AuditRepository;
import com.intellistock.repository.ProductRepository;
import com.intellistock.repository.StockMovementRepository;
import com.intellistock.security.Permissions;
import com.intellistock.util.Dates;
import com.intellistock.util.Validation;

import java.sql.Connection;
import java.util.List;

public class InventoryService {
    private final Database database;
    private final ProductRepository products = new ProductRepository();
    private final StockMovementRepository movements = new StockMovementRepository();
    private final AuditRepository audit = new AuditRepository();
    private final AlertEngine alertEngine;

    public InventoryService(Database database, AlertEngine alertEngine) {
        this.database = database;
        this.alertEngine = alertEngine;
    }

    public StockMovement receive(User user, long productId, int quantity, String reason, String reference) {
        Permissions.requireUser(user);
        return apply(user, productId, MovementType.RECEIPT, quantity, true, reason, reference, null, null);
    }

    public StockMovement dispatch(User user, long productId, int quantity, String reason, String reference) {
        Permissions.requireUser(user);
        return apply(user, productId, MovementType.DISPATCH, quantity, false, reason, reference, null, null);
    }

    public StockMovement recordReturn(User user, long productId, int quantity, String reason, String reference) {
        Permissions.requireUser(user);
        return apply(user, productId, MovementType.RETURN, quantity, true, reason, reference, null, null);
    }

    public StockMovement damaged(User user, long productId, int quantity, String reason) {
        Permissions.requireUser(user);
        return apply(user, productId, MovementType.DAMAGED, quantity, false, reason, null, null, null);
    }

    public StockMovement adjust(User user, long productId, int quantityDelta, String reason) {
        Permissions.requireAdmin(user);
        if (quantityDelta == 0) {
            throw new IllegalArgumentException("Adjustment quantity cannot be zero");
        }
        boolean inbound = quantityDelta > 0;
        return apply(user, productId, MovementType.ADJUSTMENT, Math.abs(quantityDelta), inbound, reason, null, null, null);
    }

    public StockMovement correct(User user, long productId, int quantityDelta, String reason) {
        Permissions.requireAdmin(user);
        if (quantityDelta == 0) {
            throw new IllegalArgumentException("Correction quantity cannot be zero");
        }
        return apply(user, productId, MovementType.CORRECTION, Math.abs(quantityDelta), quantityDelta > 0, reason, null, null, null);
    }

    public StockMovement apply(User user, long productId, MovementType type, int quantity, boolean inbound,
                               String reason, String reference, Long purchaseOrderId, Long receiptId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if ((type == MovementType.ADJUSTMENT || type == MovementType.CORRECTION || type == MovementType.DAMAGED)
                && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A reason is required for this movement");
        }
        StockMovement saved = database.inTransaction(connection ->
                apply(connection, user, productId, type, quantity, inbound, reason, reference, purchaseOrderId, receiptId));
        alertEngine.refresh();
        return saved;
    }

    public StockMovement apply(Connection connection, User user, long productId, MovementType type, int quantity,
                               boolean inbound, String reason, String reference, Long purchaseOrderId, Long receiptId)
            throws Exception {
        Product product = products.findById(connection, productId)
                .orElseThrow(() -> new IllegalArgumentException("Product was not found"));
        if (product.isArchived()) {
            throw new IllegalArgumentException("Archived products cannot receive stock movements");
        }
        int previous = product.getQuantity();
        int delta = inbound ? quantity : -quantity;
        int resulting = previous + delta;
        if (resulting < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: " + previous + ", requested: " + quantity);
        }
        String now = Dates.now();
        products.updateQuantity(connection, productId, resulting, now);
        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setQuantityDelta(delta);
        movement.setPreviousQuantity(previous);
        movement.setResultingQuantity(resulting);
        movement.setReason(Validation.blankToNull(reason));
        movement.setReference(Validation.blankToNull(reference));
        movement.setPurchaseOrderId(purchaseOrderId);
        movement.setPurchaseReceiptId(receiptId);
        movement.setUserId(user.getId());
        movement.setOccurredAt(now);
        long id = movements.insert(connection, movement);
        movement.setId(id);
        audit.insert(connection, user.getId(), "STOCK_" + type.name(), "PRODUCT", productId,
                "qty=" + quantity + " resulting=" + resulting);
        return movement;
    }

    public List<StockMovement> recent() {
        return database.inTransaction(connection -> movements.findRecent(connection, 50));
    }

    public List<StockMovement> history(long productId) {
        return database.inTransaction(connection -> movements.findByProduct(connection, productId));
    }

    public List<StockMovement> all() {
        return database.inTransaction(connection -> movements.findAll(connection));
    }
}
