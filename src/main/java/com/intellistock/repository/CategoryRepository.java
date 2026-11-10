package com.intellistock.repository;

import com.intellistock.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepository {
    public List<Category> findAll(Connection connection) throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM categories ORDER BY name");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                categories.add(new Category(rs.getLong("id"), rs.getString("name")));
            }
        }
        return categories;
    }

    public Optional<Category> findByName(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM categories WHERE name = ? COLLATE NOCASE")) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(new Category(rs.getLong("id"), rs.getString("name"))) : Optional.empty();
            }
        }
    }

    public long insert(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO categories(name) VALUES (?)")) {
            statement.setString(1, name);
            statement.executeUpdate();
            try (ResultSet keys = connection.prepareStatement("SELECT last_insert_rowid()").executeQuery()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }
}
