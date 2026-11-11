package com.intellistock.repository;

import com.intellistock.model.Role;
import com.intellistock.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    public Optional<User> findByUsername(Connection connection, String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? COLLATE NOCASE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<User> findAll(Connection connection) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM users ORDER BY username");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
        }
        return users;
    }

    public long insert(Connection connection, User user) throws SQLException {
        String sql = """
                INSERT INTO users(username, password_hash, full_name, role, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getFullName());
            statement.setString(4, user.getRole().name());
            statement.setInt(5, user.isActive() ? 1 : 0);
            statement.setString(6, user.getCreatedAt());
            statement.executeUpdate();
            return JdbcSupport.getLastInsertId(connection);
        }
    }

    public void update(Connection connection, User user) throws SQLException {
        String sql = """
                UPDATE users SET full_name = ?, role = ?, active = ?, password_hash = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getRole().name());
            statement.setInt(3, user.isActive() ? 1 : 0);
            statement.setString(4, user.getPasswordHash());
            statement.setLong(5, user.getId());
            statement.executeUpdate();
        }
    }

    public long count(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setActive(rs.getInt("active") == 1);
        user.setCreatedAt(rs.getString("created_at"));
        return user;
    }
}
