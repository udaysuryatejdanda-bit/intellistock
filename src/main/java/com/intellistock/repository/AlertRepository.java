package com.intellistock.repository;

import com.intellistock.model.AlertSeverity;
import com.intellistock.model.AlertStatus;
import com.intellistock.model.AlertType;
import com.intellistock.model.InventoryAlert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlertRepository {
    private static final String SELECT = """
            SELECT a.*, p.name AS product_name
            FROM alerts a
            LEFT JOIN products p ON p.id = a.product_id
            """;

    public List<InventoryAlert> findOpen(Connection connection) throws SQLException {
        List<InventoryAlert> alerts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT + " WHERE a.status IN ('ACTIVE','ACKNOWLEDGED') ORDER BY a.severity DESC, a.created_at DESC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                alerts.add(map(rs));
            }
        }
        return alerts;
    }

    public List<InventoryAlert> findAll(Connection connection) throws SQLException {
        List<InventoryAlert> alerts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT + " ORDER BY a.created_at DESC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                alerts.add(map(rs));
            }
        }
        return alerts;
    }

    public Optional<InventoryAlert> findOpen(Connection connection, Long productId, Long purchaseOrderId, AlertType type)
            throws SQLException {
        String sql = SELECT + """
                 WHERE a.alert_type = ? AND a.status IN ('ACTIVE','ACKNOWLEDGED')
                   AND ((? IS NOT NULL AND a.product_id = ?) OR (? IS NOT NULL AND a.purchase_order_id = ?))
                 LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type.name());
            JdbcSupport.setNullableLong(statement, 2, productId);
            JdbcSupport.setNullableLong(statement, 3, productId);
            JdbcSupport.setNullableLong(statement, 4, purchaseOrderId);
            JdbcSupport.setNullableLong(statement, 5, purchaseOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public long insert(Connection connection, InventoryAlert alert) throws SQLException {
        String sql = """
                INSERT INTO alerts(product_id, purchase_order_id, alert_type, severity, message, explanation,
                    status, created_at, updated_at, resolved_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setNullableLong(statement, 1, alert.getProductId());
            JdbcSupport.setNullableLong(statement, 2, alert.getPurchaseOrderId());
            statement.setString(3, alert.getAlertType().name());
            statement.setString(4, alert.getSeverity().name());
            statement.setString(5, alert.getMessage());
            JdbcSupport.setNullableString(statement, 6, alert.getExplanation());
            statement.setString(7, alert.getStatus().name());
            statement.setString(8, alert.getCreatedAt());
            statement.setString(9, alert.getUpdatedAt());
            JdbcSupport.setNullableString(statement, 10, alert.getResolvedAt());
            statement.executeUpdate();
            return JdbcSupport.getLastInsertId(connection);
        }
    }

    public void update(Connection connection, InventoryAlert alert) throws SQLException {
        String sql = """
                UPDATE alerts SET severity = ?, message = ?, explanation = ?, status = ?, updated_at = ?, resolved_at = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, alert.getSeverity().name());
            statement.setString(2, alert.getMessage());
            JdbcSupport.setNullableString(statement, 3, alert.getExplanation());
            statement.setString(4, alert.getStatus().name());
            statement.setString(5, alert.getUpdatedAt());
            JdbcSupport.setNullableString(statement, 6, alert.getResolvedAt());
            statement.setLong(7, alert.getId());
            statement.executeUpdate();
        }
    }

    public Optional<InventoryAlert> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE a.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private InventoryAlert map(ResultSet rs) throws SQLException {
        InventoryAlert alert = new InventoryAlert();
        alert.setId(rs.getLong("id"));
        alert.setProductId(JdbcSupport.getNullableLong(rs, "product_id"));
        alert.setProductName(rs.getString("product_name"));
        alert.setPurchaseOrderId(JdbcSupport.getNullableLong(rs, "purchase_order_id"));
        alert.setAlertType(AlertType.valueOf(rs.getString("alert_type")));
        alert.setSeverity(AlertSeverity.valueOf(rs.getString("severity")));
        alert.setMessage(rs.getString("message"));
        alert.setExplanation(rs.getString("explanation"));
        alert.setStatus(AlertStatus.valueOf(rs.getString("status")));
        alert.setCreatedAt(rs.getString("created_at"));
        alert.setUpdatedAt(rs.getString("updated_at"));
        alert.setResolvedAt(rs.getString("resolved_at"));
        return alert;
    }
}
