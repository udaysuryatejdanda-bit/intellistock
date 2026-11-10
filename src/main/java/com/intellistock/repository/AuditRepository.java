package com.intellistock.repository;

import com.intellistock.model.AuditLog;
import com.intellistock.util.Dates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuditRepository {
    public void insert(Connection connection, Long userId, String action, String entityType, Long entityId, String details)
            throws SQLException {
        String sql = """
                INSERT INTO audit_logs(user_id, action, entity_type, entity_id, details, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setNullableLong(statement, 1, userId);
            statement.setString(2, action);
            JdbcSupport.setNullableString(statement, 3, entityType);
            JdbcSupport.setNullableLong(statement, 4, entityId);
            JdbcSupport.setNullableString(statement, 5, details);
            statement.setString(6, Dates.now());
            statement.executeUpdate();
        }
    }

    public List<AuditLog> recent(Connection connection, int limit) throws SQLException {
        String sql = """
                SELECT a.*, u.username FROM audit_logs a
                LEFT JOIN users u ON u.id = a.user_id
                ORDER BY a.created_at DESC, a.id DESC LIMIT ?
                """;
        List<AuditLog> logs = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setId(rs.getLong("id"));
                    log.setUserId(JdbcSupport.getNullableLong(rs, "user_id"));
                    log.setUsername(rs.getString("username"));
                    log.setAction(rs.getString("action"));
                    log.setEntityType(rs.getString("entity_type"));
                    log.setEntityId(JdbcSupport.getNullableLong(rs, "entity_id"));
                    log.setDetails(rs.getString("details"));
                    log.setCreatedAt(rs.getString("created_at"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }
}
