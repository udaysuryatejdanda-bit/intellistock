package com.intellistock.repository;

import com.intellistock.model.MovementType;
import com.intellistock.model.StockMovement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockMovementRepository {
    private static final String SELECT = """
            SELECT m.*, p.name AS product_name, p.sku, u.username
            FROM stock_movements m
            JOIN products p ON p.id = m.product_id
            JOIN users u ON u.id = m.user_id
            """;

    public long insert(Connection connection, StockMovement movement) throws SQLException {
        String sql = """
                INSERT INTO stock_movements(product_id, movement_type, quantity, quantity_delta, previous_quantity,
                    resulting_quantity, reason, reference, purchase_order_id, purchase_receipt_id, user_id, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movement.getProductId());
            statement.setString(2, movement.getMovementType().name());
            statement.setInt(3, movement.getQuantity());
            statement.setInt(4, movement.getQuantityDelta());
            statement.setInt(5, movement.getPreviousQuantity());
            statement.setInt(6, movement.getResultingQuantity());
            JdbcSupport.setNullableString(statement, 7, movement.getReason());
            JdbcSupport.setNullableString(statement, 8, movement.getReference());
            JdbcSupport.setNullableLong(statement, 9, movement.getPurchaseOrderId());
            JdbcSupport.setNullableLong(statement, 10, movement.getPurchaseReceiptId());
            statement.setLong(11, movement.getUserId());
            statement.setString(12, movement.getOccurredAt());
            statement.executeUpdate();
            return JdbcSupport.getLastInsertId(connection);
        }
    }

    public List<StockMovement> findRecent(Connection connection, int limit) throws SQLException {
        return query(connection, SELECT + " ORDER BY m.occurred_at DESC, m.id DESC LIMIT ?", limit);
    }

    public List<StockMovement> findByProduct(Connection connection, long productId) throws SQLException {
        List<StockMovement> movements = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT + " WHERE m.product_id = ? ORDER BY m.occurred_at DESC, m.id DESC")) {
            statement.setLong(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    movements.add(map(rs));
                }
            }
        }
        return movements;
    }

    public List<StockMovement> findAll(Connection connection) throws SQLException {
        return query(connection, SELECT + " ORDER BY m.occurred_at DESC, m.id DESC", null);
    }

    public List<StockMovement> findOutboundBetween(Connection connection, long productId, String from, String to)
            throws SQLException {
        String sql = SELECT + """
                 WHERE m.product_id = ? AND m.quantity_delta < 0
                 AND m.occurred_at >= ? AND m.occurred_at < ?
                 ORDER BY m.occurred_at
                """;
        List<StockMovement> movements = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            statement.setString(2, from);
            statement.setString(3, to);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    movements.add(map(rs));
                }
            }
        }
        return movements;
    }

    public int sumOutbound(Connection connection, long productId, String fromInclusive, String toExclusive)
            throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(-quantity_delta), 0) FROM stock_movements
                WHERE product_id = ? AND quantity_delta < 0
                  AND occurred_at >= ? AND occurred_at < ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            statement.setString(2, fromInclusive);
            statement.setString(3, toExclusive);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<StockMovement> query(Connection connection, String sql, Integer limit) throws SQLException {
        List<StockMovement> movements = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (limit != null) {
                statement.setInt(1, limit);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    movements.add(map(rs));
                }
            }
        }
        return movements;
    }

    private StockMovement map(ResultSet rs) throws SQLException {
        StockMovement movement = new StockMovement();
        movement.setId(rs.getLong("id"));
        movement.setProductId(rs.getLong("product_id"));
        movement.setProductName(rs.getString("product_name"));
        movement.setSku(rs.getString("sku"));
        movement.setMovementType(MovementType.valueOf(rs.getString("movement_type")));
        movement.setQuantity(rs.getInt("quantity"));
        movement.setQuantityDelta(rs.getInt("quantity_delta"));
        movement.setPreviousQuantity(rs.getInt("previous_quantity"));
        movement.setResultingQuantity(rs.getInt("resulting_quantity"));
        movement.setReason(rs.getString("reason"));
        movement.setReference(rs.getString("reference"));
        movement.setPurchaseOrderId(JdbcSupport.getNullableLong(rs, "purchase_order_id"));
        movement.setPurchaseReceiptId(JdbcSupport.getNullableLong(rs, "purchase_receipt_id"));
        movement.setUserId(rs.getLong("user_id"));
        movement.setUsername(rs.getString("username"));
        movement.setOccurredAt(rs.getString("occurred_at"));
        return movement;
    }
}
