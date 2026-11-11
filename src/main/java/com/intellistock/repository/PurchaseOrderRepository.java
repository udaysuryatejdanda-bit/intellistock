package com.intellistock.repository;

import com.intellistock.model.OrderStatus;
import com.intellistock.model.PurchaseOrder;
import com.intellistock.model.PurchaseOrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PurchaseOrderRepository {
    private static final String SELECT = """
            SELECT po.*, s.company_name AS supplier_name, u.full_name AS created_by_name
            FROM purchase_orders po
            JOIN suppliers s ON s.id = po.supplier_id
            JOIN users u ON u.id = po.created_by
            """;

    public long insertOrder(Connection connection, PurchaseOrder order) throws SQLException {
        String sql = """
                INSERT INTO purchase_orders(supplier_id, status, expected_delivery_date, notes, created_by, created_at,
                    submitted_at, cancelled_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, order.getSupplierId());
            statement.setString(2, order.getStatus().name());
            JdbcSupport.setNullableString(statement, 3, order.getExpectedDeliveryDate());
            JdbcSupport.setNullableString(statement, 4, order.getNotes());
            statement.setLong(5, order.getCreatedBy());
            statement.setString(6, order.getCreatedAt());
            JdbcSupport.setNullableString(statement, 7, order.getSubmittedAt());
            JdbcSupport.setNullableString(statement, 8, order.getCancelledAt());
            statement.executeUpdate();
            return JdbcSupport.getLastInsertId(connection);
        }
    }

    public void insertItem(Connection connection, PurchaseOrderItem item) throws SQLException {
        String sql = """
                INSERT INTO purchase_order_items(purchase_order_id, product_id, quantity_ordered, quantity_received,
                    unit_cost_cents)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, item.getPurchaseOrderId());
            statement.setLong(2, item.getProductId());
            statement.setInt(3, item.getQuantityOrdered());
            statement.setInt(4, item.getQuantityReceived());
            statement.setLong(5, item.getUnitCostCents());
            statement.executeUpdate();
        }
    }

    public void updateStatus(Connection connection, long orderId, OrderStatus status, String submittedAt,
                             String cancelledAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE purchase_orders SET status = ?, submitted_at = COALESCE(?, submitted_at), cancelled_at = ? WHERE id = ?")) {
            statement.setString(1, status.name());
            JdbcSupport.setNullableString(statement, 2, submittedAt);
            JdbcSupport.setNullableString(statement, 3, cancelledAt);
            statement.setLong(4, orderId);
            statement.executeUpdate();
        }
    }

    public void addReceived(Connection connection, long itemId, int extra) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE purchase_order_items SET quantity_received = quantity_received + ? WHERE id = ?")) {
            statement.setInt(1, extra);
            statement.setLong(2, itemId);
            statement.executeUpdate();
        }
    }

    public long insertReceipt(Connection connection, long orderId, long userId, String receivedAt, String notes,
                              String idempotencyKey) throws SQLException {
        String sql = """
                INSERT INTO purchase_receipts(purchase_order_id, received_by, received_at, notes, idempotency_key)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, userId);
            statement.setString(3, receivedAt);
            JdbcSupport.setNullableString(statement, 4, notes);
            statement.setString(5, idempotencyKey);
            statement.executeUpdate();
            return JdbcSupport.getLastInsertId(connection);
        }
    }

    public void insertReceiptItem(Connection connection, long receiptId, long productId, int quantity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO purchase_receipt_items(receipt_id, product_id, quantity) VALUES (?, ?, ?)")) {
            statement.setLong(1, receiptId);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            statement.executeUpdate();
        }
    }

    public boolean receiptKeyExists(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM purchase_receipts WHERE idempotency_key = ?")) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Optional<PurchaseOrder> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE po.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                PurchaseOrder order = mapOrder(rs);
                order.setItems(findItems(connection, id));
                return Optional.of(order);
            }
        }
    }

    public List<PurchaseOrder> findAll(Connection connection) throws SQLException {
        List<PurchaseOrder> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT + " ORDER BY po.created_at DESC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                PurchaseOrder order = mapOrder(rs);
                order.setItems(findItems(connection, order.getId()));
                orders.add(order);
            }
        }
        return orders;
    }

    public List<PurchaseOrder> findBySupplier(Connection connection, long supplierId) throws SQLException {
        List<PurchaseOrder> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT + " WHERE po.supplier_id = ? ORDER BY po.created_at DESC")) {
            statement.setLong(1, supplierId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrder order = mapOrder(rs);
                    order.setItems(findItems(connection, order.getId()));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    public List<PurchaseOrder> findByProduct(Connection connection, long productId) throws SQLException {
        String sql = SELECT + """
                 WHERE po.id IN (SELECT purchase_order_id FROM purchase_order_items WHERE product_id = ?)
                 ORDER BY po.created_at DESC
                """;
        List<PurchaseOrder> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrder order = mapOrder(rs);
                    order.setItems(findItems(connection, order.getId()));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    public List<PurchaseOrder> findByStatus(Connection connection, OrderStatus status) throws SQLException {
        List<PurchaseOrder> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT + " WHERE po.status = ? ORDER BY po.created_at DESC")) {
            statement.setString(1, status.name());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrder order = mapOrder(rs);
                    order.setItems(findItems(connection, order.getId()));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    public List<PurchaseOrderItem> findItems(Connection connection, long orderId) throws SQLException {
        String sql = """
                SELECT i.*, p.name AS product_name, p.sku
                FROM purchase_order_items i
                JOIN products p ON p.id = i.product_id
                WHERE i.purchase_order_id = ?
                """;
        List<PurchaseOrderItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrderItem item = new PurchaseOrderItem();
                    item.setId(rs.getLong("id"));
                    item.setPurchaseOrderId(rs.getLong("purchase_order_id"));
                    item.setProductId(rs.getLong("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setSku(rs.getString("sku"));
                    item.setQuantityOrdered(rs.getInt("quantity_ordered"));
                    item.setQuantityReceived(rs.getInt("quantity_received"));
                    item.setUnitCostCents(rs.getLong("unit_cost_cents"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private PurchaseOrder mapOrder(ResultSet rs) throws SQLException {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(rs.getLong("id"));
        order.setSupplierId(rs.getLong("supplier_id"));
        order.setSupplierName(rs.getString("supplier_name"));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setExpectedDeliveryDate(rs.getString("expected_delivery_date"));
        order.setNotes(rs.getString("notes"));
        order.setCreatedBy(rs.getLong("created_by"));
        order.setCreatedByName(rs.getString("created_by_name"));
        order.setCreatedAt(rs.getString("created_at"));
        order.setSubmittedAt(rs.getString("submitted_at"));
        order.setCancelledAt(rs.getString("cancelled_at"));
        return order;
    }
}
