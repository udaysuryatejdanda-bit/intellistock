package com.intellistock.service;

import com.intellistock.alerts.AlertEngine;
import com.intellistock.config.Database;
import com.intellistock.model.Category;
import com.intellistock.model.MovementType;
import com.intellistock.model.Product;
import com.intellistock.model.ProductRegistrationRequest;
import com.intellistock.model.User;
import com.intellistock.repository.AuditRepository;
import com.intellistock.repository.CategoryRepository;
import com.intellistock.repository.ProductRepository;
import com.intellistock.repository.PurchaseOrderRepository;
import com.intellistock.security.Permissions;
import com.intellistock.util.Dates;
import com.intellistock.util.Validation;

import java.sql.Connection;
import java.util.List;

public class ProductService {
    private final Database database;
    private final ProductRepository products = new ProductRepository();
    private final CategoryRepository categories = new CategoryRepository();
    private final PurchaseOrderRepository purchaseOrders = new PurchaseOrderRepository();
    private final AuditRepository audit = new AuditRepository();
    private final InventoryService inventory;
    private final AlertEngine alertEngine;

    public ProductService(Database database, InventoryService inventory, AlertEngine alertEngine) {
        this.database = database;
        this.inventory = inventory;
        this.alertEngine = alertEngine;
    }

    public Product register(User user, ProductRegistrationRequest request) {
        Permissions.requireUser(user);
        if (!Permissions.canRegisterProducts(user)) {
            throw new SecurityException("You are not allowed to register products");
        }
        validate(request);
        Product saved = database.inTransaction(connection -> {
            if (products.findBySku(connection, request.getSku()).isPresent()) {
                throw new IllegalArgumentException("SKU already exists");
            }
            if (request.getBarcode() != null && products.findByBarcode(connection, request.getBarcode()).isPresent()) {
                throw new IllegalArgumentException("Barcode already exists");
            }
            long categoryId = resolveCategory(connection, request);
            Product product = toProduct(request, categoryId);
            long id = products.insert(connection, product);
            product.setId(id);
            if (request.getInitialQuantity() > 0) {
                inventory.apply(connection, user, id, MovementType.INITIAL_STOCK, request.getInitialQuantity(),
                        true, "Opening stock on product registration", null, null, null);
            }
            audit.insert(connection, user.getId(), "PRODUCT_REGISTER", "PRODUCT", id, product.getSku());
            return products.findById(connection, id).orElse(product);
        });
        alertEngine.refresh();
        return saved;
    }

    public Product updateMetadata(User user, Product product) {
        Permissions.requireUser(user);
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (product.getReorderThreshold() < 0 || product.getCriticalThreshold() < 0) {
            throw new IllegalArgumentException("Thresholds cannot be negative");
        }
        if (product.getCriticalThreshold() > product.getReorderThreshold()) {
            throw new IllegalArgumentException("Critical threshold cannot exceed the reorder threshold");
        }
        Product updated = database.inTransaction(connection -> {
            Product existing = products.findById(connection, product.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product was not found"));
            if (product.getBarcode() != null) {
                products.findByBarcode(connection, product.getBarcode()).ifPresent(other -> {
                    if (other.getId() != product.getId()) {
                        throw new IllegalArgumentException("Barcode already exists");
                    }
                });
            }
            existing.setName(product.getName().trim());
            existing.setCategoryId(product.getCategoryId());
            existing.setDescription(product.getDescription());
            existing.setUnit(product.getUnit());
            existing.setBarcode(Validation.blankToNull(product.getBarcode()));
            existing.setReorderThreshold(product.getReorderThreshold());
            existing.setCriticalThreshold(product.getCriticalThreshold());
            existing.setWarehouseLocation(product.getWarehouseLocation());
            existing.setRackShelf(product.getRackShelf());
            existing.setExpiryDate(product.getExpiryDate());
            existing.setCostPriceCents(product.getCostPriceCents());
            existing.setSellingPriceCents(product.getSellingPriceCents());
            existing.setPreferredSupplierId(product.getPreferredSupplierId());
            existing.setUpdatedAt(Dates.now());
            products.updateMetadata(connection, existing);
            audit.insert(connection, user.getId(), "PRODUCT_UPDATE", "PRODUCT", existing.getId(),
                    "Metadata updated; quantity unchanged at " + existing.getQuantity());
            return products.findById(connection, existing.getId()).orElse(existing);
        });
        alertEngine.refresh();
        return updated;
    }

    public void archive(User user, long productId) {
        Permissions.requireUser(user);
        database.execute(connection -> {
            Product product = products.findById(connection, productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product was not found"));
            products.setArchived(connection, productId, true, Dates.now());
            audit.insert(connection, user.getId(), "PRODUCT_ARCHIVE", "PRODUCT", productId, product.getSku());
        });
        alertEngine.refresh();
    }

    public List<Product> list(boolean includeArchived) {
        return database.inTransaction(connection -> products.findAll(connection, includeArchived));
    }

    public List<Product> search(String query) {
        return database.inTransaction(connection -> products.search(connection, query == null ? "" : query.trim()));
    }

    public Product get(long id) {
        return database.inTransaction(connection -> products.findById(connection, id)
                .orElseThrow(() -> new IllegalArgumentException("Product was not found")));
    }

    public List<Category> categories() {
        return database.inTransaction(categories::findAll);
    }

    public Object purchaseOrdersFor(long productId) {
        return database.inTransaction(connection -> purchaseOrders.findByProduct(connection, productId));
    }

    private void validate(ProductRegistrationRequest request) {
        Validation.required("Product name", request.getName());
        Validation.required("SKU", request.getSku());
        Validation.required("Unit", request.getUnit());
        if (request.getInitialQuantity() < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative");
        }
        if (request.getReorderThreshold() < 0) {
            throw new IllegalArgumentException("Reorder threshold cannot be negative");
        }
        int critical = request.getCriticalThreshold() == null
                ? Math.max(1, request.getReorderThreshold() / 2)
                : request.getCriticalThreshold();
        if (critical < 0) {
            throw new IllegalArgumentException("Critical threshold cannot be negative");
        }
        if (critical > request.getReorderThreshold() && request.getReorderThreshold() > 0) {
            throw new IllegalArgumentException("Critical threshold cannot exceed the reorder threshold");
        }
        request.setCriticalThreshold(critical);
        if (request.getCostPriceCents() < 0 || request.getSellingPriceCents() < 0) {
            throw new IllegalArgumentException("Prices cannot be negative");
        }
        if (request.getExpiryDate() != null && Dates.parseDate(request.getExpiryDate()).isEmpty()) {
            throw new IllegalArgumentException("Expiry date must use YYYY-MM-DD");
        }
        if (request.getCategoryId() == null && (request.getNewCategoryName() == null || request.getNewCategoryName().isBlank())) {
            throw new IllegalArgumentException("Category is required");
        }
        request.setBarcode(Validation.blankToNull(request.getBarcode()));
        request.setName(request.getName().trim());
        request.setSku(request.getSku().trim());
    }

    private long resolveCategory(Connection connection, ProductRegistrationRequest request) throws Exception {
        if (request.getNewCategoryName() != null && !request.getNewCategoryName().isBlank()) {
            return categories.findByName(connection, request.getNewCategoryName().trim())
                    .map(Category::getId)
                    .orElseGet(() -> {
                        try {
                            return categories.insert(connection, request.getNewCategoryName().trim());
                        } catch (Exception error) {
                            throw new IllegalStateException(error);
                        }
                    });
        }
        return request.getCategoryId();
    }

    private Product toProduct(ProductRegistrationRequest request, long categoryId) {
        Product product = new Product();
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setCategoryId(categoryId);
        product.setDescription(Validation.blankToNull(request.getDescription()));
        product.setUnit(request.getUnit());
        product.setBarcode(request.getBarcode());
        product.setQuantity(0);
        product.setReorderThreshold(request.getReorderThreshold());
        product.setCriticalThreshold(request.getCriticalThreshold());
        product.setWarehouseLocation(Validation.blankToNull(request.getWarehouseLocation()));
        product.setRackShelf(Validation.blankToNull(request.getRackShelf()));
        product.setExpiryDate(Validation.blankToNull(request.getExpiryDate()));
        product.setCostPriceCents(request.getCostPriceCents());
        product.setSellingPriceCents(request.getSellingPriceCents());
        product.setPreferredSupplierId(request.getPreferredSupplierId());
        String now = Dates.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        return product;
    }
}
