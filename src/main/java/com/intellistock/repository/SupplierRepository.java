package com.intellistock.repository;

import com.intellistock.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SupplierRepository {
    public List<Supplier> findAll(Connection connection, boolean activeOnly) throws SQLException {
        String sql = activeOnly
                ? "SELECT * FROM suppliers WHERE active = 1 ORDER BY company_name"
                : "SELECT * FROM suppliers ORDER BY company_name";
        List<Supplier> suppliers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                suppliers.add(map(rs));
            }
        }
        return suppliers;
    }

    public Optional<Supplier> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM suppliers WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Supplier> search(Connection connection, String query) throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = """
                SELECT * FROM suppliers
                WHERE company_name LIKE ? OR contact_person LIKE ? OR email LIKE ? OR phone LIKE ?
                ORDER BY company_name
                """;
        String like = "%" + query + "%";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    suppliers.add(map(rs));
                }
            }
        }
        return suppliers;
    }

    public long insert(Connection connection, Supplier supplier) throws SQLException {
        String sql = """
                INSERT INTO suppliers(company_name, contact_person, email, phone, address, notes, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, supplier);
            statement.executeUpdate();
            return JdbcSupport.getLastInsertId(connection);
        }
    }

    public void update(Connection connection, Supplier supplier) throws SQLException {
        String sql = """
                UPDATE suppliers SET company_name = ?, contact_person = ?, email = ?, phone = ?,
                address = ?, notes = ?, active = ? WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, supplier.getCompanyName());
            JdbcSupport.setNullableString(statement, 2, supplier.getContactPerson());
            JdbcSupport.setNullableString(statement, 3, supplier.getEmail());
            JdbcSupport.setNullableString(statement, 4, supplier.getPhone());
            JdbcSupport.setNullableString(statement, 5, supplier.getAddress());
            JdbcSupport.setNullableString(statement, 6, supplier.getNotes());
            statement.setInt(7, supplier.isActive() ? 1 : 0);
            statement.setLong(8, supplier.getId());
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, Supplier supplier) throws SQLException {
        statement.setString(1, supplier.getCompanyName());
        JdbcSupport.setNullableString(statement, 2, supplier.getContactPerson());
        JdbcSupport.setNullableString(statement, 3, supplier.getEmail());
        JdbcSupport.setNullableString(statement, 4, supplier.getPhone());
        JdbcSupport.setNullableString(statement, 5, supplier.getAddress());
        JdbcSupport.setNullableString(statement, 6, supplier.getNotes());
        statement.setInt(7, supplier.isActive() ? 1 : 0);
        statement.setString(8, supplier.getCreatedAt());
    }

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setId(rs.getLong("id"));
        supplier.setCompanyName(rs.getString("company_name"));
        supplier.setContactPerson(rs.getString("contact_person"));
        supplier.setEmail(rs.getString("email"));
        supplier.setPhone(rs.getString("phone"));
        supplier.setAddress(rs.getString("address"));
        supplier.setNotes(rs.getString("notes"));
        supplier.setActive(rs.getInt("active") == 1);
        supplier.setCreatedAt(rs.getString("created_at"));
        return supplier;
    }
}
