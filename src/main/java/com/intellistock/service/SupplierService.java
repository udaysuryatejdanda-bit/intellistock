package com.intellistock.service;

import com.intellistock.config.Database;
import com.intellistock.model.Product;
import com.intellistock.model.Supplier;
import com.intellistock.model.User;
import com.intellistock.repository.AuditRepository;
import com.intellistock.repository.ProductRepository;
import com.intellistock.repository.SupplierRepository;
import com.intellistock.security.Permissions;
import com.intellistock.util.Dates;
import com.intellistock.util.Validation;

import java.util.List;
import java.util.stream.Collectors;

public class SupplierService {
    private final Database database;
    private final SupplierRepository suppliers = new SupplierRepository();
    private final ProductRepository products = new ProductRepository();
    private final AuditRepository audit = new AuditRepository();

    public SupplierService(Database database) {
        this.database = database;
    }

    public Supplier save(User user, Supplier supplier) {
        Permissions.requireUser(user);
        Validation.required("Company name", supplier.getCompanyName());
        Validation.optionalEmail(supplier.getEmail());
        Validation.optionalPhone(supplier.getPhone());
        return database.inTransaction(connection -> {
            if (supplier.getId() == 0) {
                supplier.setCreatedAt(Dates.now());
                supplier.setActive(true);
                long id = suppliers.insert(connection, supplier);
                supplier.setId(id);
                audit.insert(connection, user.getId(), "SUPPLIER_CREATE", "SUPPLIER", id, supplier.getCompanyName());
            } else {
                suppliers.update(connection, supplier);
                audit.insert(connection, user.getId(), "SUPPLIER_UPDATE", "SUPPLIER", supplier.getId(),
                        supplier.getCompanyName());
            }
            return suppliers.findById(connection, supplier.getId()).orElse(supplier);
        });
    }

    public List<Supplier> list(boolean activeOnly) {
        return database.inTransaction(connection -> suppliers.findAll(connection, activeOnly));
    }

    public List<Supplier> search(String query) {
        return database.inTransaction(connection -> suppliers.search(connection, query == null ? "" : query.trim()));
    }

    public List<Product> productsFor(long supplierId) {
        return database.inTransaction(connection -> products.findAll(connection, true).stream()
                .filter(product -> product.getPreferredSupplierId() != null
                        && product.getPreferredSupplierId() == supplierId)
                .collect(Collectors.toList()));
    }
}
