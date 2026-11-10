package com.intellistock.repository;

import com.intellistock.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductRepository {
    private static final String SELECT = """
            SELECT p.*, c.name AS category_name, s.company_name AS supplier_name
            FROM products p
            JOIN categories c ON c.id = p.category_id
            LEFT JOIN suppliers s ON s.id = p.preferred_supplier_id
            """;

    public List<Product> findAll(Connection connection, boolean includeArchived) throws SQLException {
        String sql = SELECT + (includeArchived ? "" : " WHERE p.archived = 0") + " ORDER BY p.name";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                products.add(map(rs));
            }
        }
        return products;
    }

    public Optional<Product> findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE p.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Product> findBySku(Connection connection, String sku) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE p.sku = ? COLLATE NOCASE")) {
            statement.setString(1, sku);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Product> findByBarcode(Connection connection, String barcode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT + " WHERE p.barcode = ? AND p.barcode IS NOT NULL AND TRIM(p.barcode) <> ''")) {
            statement.setString(1, barcode);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Product> search(Connection connection, String query) throws SQLException {
        String sql = SELECT + """
                 WHERE p.archived = 0 AND (
                    p.name LIKE ? OR p.sku LIKE ? OR IFNULL(p.barcode, '') LIKE ?
                 ) ORDER BY p.name
                """;
        String like = "%" + query + "%";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setString(2, like);
            statement.setString(3, like);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    products.add(map(rs));
                }
            }
        }
        return products;
    }

    public long insert(Connection connection, Product product) throws SQLException {
        String sql = """
                INSERT INTO products(sku, name, category_id, description, unit, barcode, quantity,
                    reorder_threshold, critical_threshold, warehouse_location, rack_shelf, expiry_date,
                    cost_price_cents, selling_price_cents, preferred_supplier_id, archived, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, product, false);
            statement.executeUpdate();
            try (ResultSet keys = connection.prepareStatement("SELECT last_insert_rowid()").executeQuery()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updateMetadata(Connection connection, Product product) throws SQLException {
        String sql = """
                UPDATE products SET name = ?, category_id = ?, description = ?, unit = ?, barcode = ?,
                    reorder_threshold = ?, critical_threshold = ?, warehouse_location = ?, rack_shelf = ?,
                    expiry_date = ?, cost_price_cents = ?, selling_price_cents = ?, preferred_supplier_id = ?,
                    updated_at = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getName());
            statement.setLong(2, product.getCategoryId());
            JdbcSupport.setNullableString(statement, 3, product.getDescription());
            statement.setString(4, product.getUnit());
            JdbcSupport.setNullableString(statement, 5, product.getBarcode());
            statement.setInt(6, product.getReorderThreshold());
            statement.setInt(7, product.getCriticalThreshold());
            JdbcSupport.setNullableString(statement, 8, product.getWarehouseLocation());
            JdbcSupport.setNullableString(statement, 9, product.getRackShelf());
            JdbcSupport.setNullableString(statement, 10, product.getExpiryDate());
            statement.setLong(11, product.getCostPriceCents());
            statement.setLong(12, product.getSellingPriceCents());
            JdbcSupport.setNullableLong(statement, 13, product.getPreferredSupplierId());
            statement.setString(14, product.getUpdatedAt());
            statement.setLong(15, product.getId());
            statement.executeUpdate();
        }
    }

    public void updateQuantity(Connection connection, long productId, int quantity, String updatedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET quantity = ?, updated_at = ? WHERE id = ?")) {
            statement.setInt(1, quantity);
            statement.setString(2, updatedAt);
            statement.setLong(3, productId);
            statement.executeUpdate();
        }
    }

    public void setArchived(Connection connection, long productId, boolean archived, String updatedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET archived = ?, updated_at = ? WHERE id = ?")) {
            statement.setInt(1, archived ? 1 : 0);
            statement.setString(2, updatedAt);
            statement.setLong(3, productId);
            statement.executeUpdate();
        }
    }

    public boolean hasMovements(Connection connection, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM stock_movements WHERE product_id = ? LIMIT 1")) {
            statement.setLong(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countActive(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM products WHERE archived = 0");
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void bind(PreparedStatement statement, Product product, boolean update) throws SQLException {
        statement.setString(1, product.getSku());
        statement.setString(2, product.getName());
        statement.setLong(3, product.getCategoryId());
        JdbcSupport.setNullableString(statement, 4, product.getDescription());
        statement.setString(5, product.getUnit());
        JdbcSupport.setNullableString(statement, 6, product.getBarcode());
        statement.setInt(7, product.getQuantity());
        statement.setInt(8, product.getReorderThreshold());
        statement.setInt(9, product.getCriticalThreshold());
        JdbcSupport.setNullableString(statement, 10, product.getWarehouseLocation());
        JdbcSupport.setNullableString(statement, 11, product.getRackShelf());
        JdbcSupport.setNullableString(statement, 12, product.getExpiryDate());
        statement.setLong(13, product.getCostPriceCents());
        statement.setLong(14, product.getSellingPriceCents());
        JdbcSupport.setNullableLong(statement, 15, product.getPreferredSupplierId());
        statement.setInt(16, product.isArchived() ? 1 : 0);
        statement.setString(17, product.getCreatedAt());
        statement.setString(18, product.getUpdatedAt());
    }

    private Product map(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setSku(rs.getString("sku"));
        product.setName(rs.getString("name"));
        product.setCategoryId(rs.getLong("category_id"));
        product.setCategoryName(rs.getString("category_name"));
        product.setDescription(rs.getString("description"));
        product.setUnit(rs.getString("unit"));
        product.setBarcode(rs.getString("barcode"));
        product.setQuantity(rs.getInt("quantity"));
        product.setReorderThreshold(rs.getInt("reorder_threshold"));
        product.setCriticalThreshold(rs.getInt("critical_threshold"));
        product.setWarehouseLocation(rs.getString("warehouse_location"));
        product.setRackShelf(rs.getString("rack_shelf"));
        product.setExpiryDate(rs.getString("expiry_date"));
        product.setCostPriceCents(rs.getLong("cost_price_cents"));
        product.setSellingPriceCents(rs.getLong("selling_price_cents"));
        product.setPreferredSupplierId(JdbcSupport.getNullableLong(rs, "preferred_supplier_id"));
        product.setPreferredSupplierName(rs.getString("supplier_name"));
        product.setArchived(rs.getInt("archived") == 1);
        product.setCreatedAt(rs.getString("created_at"));
        product.setUpdatedAt(rs.getString("updated_at"));
        return product;
    }
}
