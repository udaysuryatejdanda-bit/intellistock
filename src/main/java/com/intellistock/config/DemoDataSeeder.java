package com.intellistock.config;

import com.intellistock.alerts.AlertEngine;
import com.intellistock.model.MovementType;
import com.intellistock.model.OrderStatus;
import com.intellistock.model.Product;
import com.intellistock.model.PurchaseOrder;
import com.intellistock.model.PurchaseOrderItem;
import com.intellistock.model.Role;
import com.intellistock.model.Supplier;
import com.intellistock.model.User;
import com.intellistock.repository.CategoryRepository;
import com.intellistock.repository.ProductRepository;
import com.intellistock.repository.PurchaseOrderRepository;
import com.intellistock.repository.SettingsRepository;
import com.intellistock.repository.SupplierRepository;
import com.intellistock.repository.UserRepository;
import com.intellistock.security.PasswordHasher;
import com.intellistock.service.InventoryService;
import com.intellistock.util.Dates;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds demonstration users, catalog, movements, and purchase orders once.
 * Demo passwords are hashed with PBKDF2 before insert. Change them from User Management.
 * Reset with the Settings page or by launching with --reset-demo (this wipes the app database).
 */
public class DemoDataSeeder {
    public static final String DEMO_ADMIN_USERNAME = "admin";
    public static final String DEMO_STAFF_USERNAME = "staff";
    static final String DEMO_ADMIN_PASSWORD = "Admin@123";
    static final String DEMO_STAFF_PASSWORD = "Staff@123";

    private final Database database;
    private final PasswordHasher hasher = new PasswordHasher();
    private final UserRepository users = new UserRepository();
    private final CategoryRepository categories = new CategoryRepository();
    private final SupplierRepository suppliers = new SupplierRepository();
    private final ProductRepository products = new ProductRepository();
    private final PurchaseOrderRepository orders = new PurchaseOrderRepository();
    private final SettingsRepository settings = new SettingsRepository();

    public DemoDataSeeder(Database database) {
        this.database = database;
    }

    public void seedIfNeeded(InventoryService inventory, AlertEngine alertEngine) {
        database.execute(connection -> {
            if (settings.get(connection, "demo.seeded").isPresent()) {
                return;
            }
            seed(connection, inventory);
            settings.put(connection, "demo.seeded", "true");
            settings.put(connection, "demand.window.days", "14");
            settings.put(connection, "demand.high.ratio", "1.5");
            settings.put(connection, "alerts.expiry.days", "30");
            settings.put(connection, "supplier.lead.days", "7");
        });
        alertEngine.refresh();
    }

    public void seed(Connection connection, InventoryService inventory) throws Exception {
        User admin = insertUser(connection, DEMO_ADMIN_USERNAME, "Demonstration Administrator", Role.ADMIN, DEMO_ADMIN_PASSWORD);
        insertUser(connection, DEMO_STAFF_USERNAME, "Demonstration Staff", Role.STAFF, DEMO_STAFF_PASSWORD);

        long stationery = categories.insert(connection, "Stationery");
        long electronics = categories.insert(connection, "Electronics");
        long groceries = categories.insert(connection, "Groceries");
        long cleaning = categories.insert(connection, "Cleaning");
        long packaging = categories.insert(connection, "Packaging");

        long northwind = supplier(connection, "Northwind Wholesale", "Priya Shah", "priya@northwind.example", "9876543210");
        long apex = supplier(connection, "Apex Components", "Rahul Mehta", "rahul@apex.example", "9123456780");
        long greenleaf = supplier(connection, "Greenleaf Foods", "Anita Rao", "anita@greenleaf.example", "9988776655");

        Product p1 = product(connection, "A4 Copier Paper", "STA-A4-001", stationery, "reams", 40, 20, 8, 25000, 32000, northwind, "WH-A", "R1-S2", null);
        Product p2 = product(connection, "Blue Ballpoint Pens", "STA-PEN-002", stationery, "boxes", 8, 15, 5, 4500, 7000, northwind, "WH-A", "R1-S4", null);
        Product p3 = product(connection, "Stapler Heavy Duty", "STA-STP-003", stationery, "pieces", 0, 6, 2, 18000, 25000, northwind, "WH-A", "R2-S1", null);
        Product p4 = product(connection, "USB-C Hub 7-in-1", "ELC-HUB-010", electronics, "pieces", 12, 5, 2, 120000, 165000, apex, "WH-B", "R3-S1", null);
        Product p5 = product(connection, "Wireless Mouse", "ELC-MOU-011", electronics, "pieces", 4, 10, 3, 45000, 69900, apex, "WH-B", "R3-S2", null);
        Product p6 = product(connection, "HDMI Cable 2m", "ELC-HDM-012", electronics, "pieces", 25, 10, 4, 18000, 29900, apex, "WH-B", "R3-S3", null);
        Product p7 = product(connection, "Basmati Rice 5kg", "GRO-RIC-020", groceries, "bags", 18, 12, 4, 42000, 56000, greenleaf, "WH-C", "R4-S1", LocalDate.now().plusDays(120).toString());
        Product p8 = product(connection, "Sunflower Oil 1L", "GRO-OIL-021", groceries, "bottles", 3, 10, 3, 14000, 18500, greenleaf, "WH-C", "R4-S2", LocalDate.now().plusDays(12).toString());
        Product p9 = product(connection, "Instant Coffee 200g", "GRO-COF-022", groceries, "jars", 0, 8, 2, 22000, 31000, greenleaf, "WH-C", "R4-S3", LocalDate.now().minusDays(3).toString());
        Product p10 = product(connection, "Floor Cleaner 5L", "CLN-FLR-030", cleaning, "cans", 9, 6, 2, 28000, 39000, northwind, "WH-A", "R5-S1", LocalDate.now().plusDays(400).toString());
        Product p11 = product(connection, "Nitrile Gloves M", "CLN-GLV-031", cleaning, "boxes", 2, 8, 3, 16000, 24000, apex, "WH-A", "R5-S2", null);
        Product p12 = product(connection, "Corrugated Boxes 12x10", "PKG-BOX-040", packaging, "pieces", 60, 25, 10, 3500, 5500, northwind, "WH-D", "R6-S1", null);

        applyHistory(connection, inventory, admin, p1, 40);
        applyHistory(connection, inventory, admin, p2, 8);
        applyHistory(connection, inventory, admin, p3, 0);
        applyHistory(connection, inventory, admin, p4, 12);
        applyHistory(connection, inventory, admin, p5, 4);
        applyHistory(connection, inventory, admin, p6, 25);
        applyHistory(connection, inventory, admin, p7, 18);
        applyHistory(connection, inventory, admin, p8, 3);
        applyHistory(connection, inventory, admin, p9, 0);
        applyHistory(connection, inventory, admin, p10, 9);
        applyHistory(connection, inventory, admin, p11, 2);
        applyHistory(connection, inventory, admin, p12, 60);

        seedMovements(connection, inventory, admin, p2, p5, p8, p11, p6, p12);

        PurchaseOrder pending = new PurchaseOrder();
        pending.setSupplierId(greenleaf);
        pending.setStatus(OrderStatus.PENDING);
        pending.setExpectedDeliveryDate(LocalDate.now().minusDays(2).toString());
        pending.setNotes("Urgent grocery replenishment");
        pending.setCreatedBy(admin.getId());
        pending.setCreatedAt(Dates.now());
        pending.setSubmittedAt(Dates.now());
        long po1 = orders.insertOrder(connection, pending);
        addItem(connection, po1, p8.getId(), 24, 14000);
        addItem(connection, po1, p9.getId(), 16, 22000);

        PurchaseOrder draft = new PurchaseOrder();
        draft.setSupplierId(apex);
        draft.setStatus(OrderStatus.DRAFT);
        draft.setExpectedDeliveryDate(LocalDate.now().plusDays(10).toString());
        draft.setNotes("Electronics restock");
        draft.setCreatedBy(admin.getId());
        draft.setCreatedAt(Dates.now());
        long po2 = orders.insertOrder(connection, draft);
        addItem(connection, po2, p5.getId(), 20, 45000);

        PurchaseOrder received = new PurchaseOrder();
        received.setSupplierId(northwind);
        received.setStatus(OrderStatus.RECEIVED);
        received.setExpectedDeliveryDate(LocalDate.now().minusDays(20).toString());
        received.setCreatedBy(admin.getId());
        received.setCreatedAt(past(25));
        received.setSubmittedAt(past(24));
        long po3 = orders.insertOrder(connection, received);
        PurchaseOrderItem paper = new PurchaseOrderItem();
        paper.setPurchaseOrderId(po3);
        paper.setProductId(p1.getId());
        paper.setQuantityOrdered(20);
        paper.setQuantityReceived(20);
        paper.setUnitCostCents(25000);
        orders.insertItem(connection, paper);
    }

    private void seedMovements(Connection connection, InventoryService inventory, User admin,
                               Product pens, Product mouse, Product oil, Product gloves, Product hdmi, Product boxes)
            throws Exception {
        for (int day = 28; day >= 1; day--) {
            if (day % 2 == 0) {
                timedDispatch(connection, inventory, admin, pens, 1, day, "Counter sales");
            }
            if (day <= 12) {
                timedDispatch(connection, inventory, admin, mouse, 1, day, "IT issue");
            }
            if (day % 3 == 0) {
                timedDispatch(connection, inventory, admin, oil, 1, day, "Canteen use");
            }
            if (day % 4 == 0) {
                timedDispatch(connection, inventory, admin, gloves, 1, day, "Housekeeping");
            }
            if (day % 5 == 0) {
                timedDispatch(connection, inventory, admin, hdmi, 2, day, "Classroom kit");
            }
            if (day % 6 == 0) {
                timedDispatch(connection, inventory, admin, boxes, 5, day, "Dispatch packing");
            }
        }
        inventory.apply(connection, admin, pens.getId(), MovementType.RECEIPT, 10, true, "Supplier top-up", "GRN-1001", null, null);
    }

    private void timedDispatch(Connection connection, InventoryService inventory, User user, Product product,
                               int qty, int daysAgo, String reason) throws Exception {
        try {
            var movement = inventory.apply(connection, user, product.getId(), MovementType.DISPATCH, qty, false,
                    reason, "SO-DEMO", null, null);
            try (var statement = connection.prepareStatement(
                    "UPDATE stock_movements SET occurred_at = ? WHERE id = ?")) {
                statement.setString(1, past(daysAgo));
                statement.setLong(2, movement.getId());
                statement.executeUpdate();
            }
        } catch (IllegalArgumentException ignored) {
            // Skip if a demo product has already reached zero.
        }
    }

    private void applyHistory(Connection connection, InventoryService inventory, User user, Product product, int qty)
            throws Exception {
        if (qty > 0) {
            var movement = inventory.apply(connection, user, product.getId(), MovementType.INITIAL_STOCK, qty, true,
                    "Demonstration opening balance", "DEMO-OPEN", null, null);
            try (var statement = connection.prepareStatement(
                    "UPDATE stock_movements SET occurred_at = ? WHERE id = ?")) {
                statement.setString(1, past(40));
                statement.setLong(2, movement.getId());
                statement.executeUpdate();
            }
        }
    }

    private Product product(Connection connection, String name, String sku, long category, String unit, int qty,
                            int reorder, int critical, long cost, long selling, long supplier, String loc, String rack,
                            String expiry) throws Exception {
        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setCategoryId(category);
        product.setUnit(unit);
        product.setQuantity(0);
        product.setReorderThreshold(reorder);
        product.setCriticalThreshold(critical);
        product.setCostPriceCents(cost);
        product.setSellingPriceCents(selling);
        product.setPreferredSupplierId(supplier);
        product.setWarehouseLocation(loc);
        product.setRackShelf(rack);
        product.setExpiryDate(expiry);
        product.setBarcode("DEMO" + sku.replace("-", ""));
        product.setCreatedAt(Dates.now());
        product.setUpdatedAt(Dates.now());
        long id = products.insert(connection, product);
        product.setId(id);
        return product;
    }

    private long supplier(Connection connection, String name, String contact, String email, String phone) throws Exception {
        Supplier supplier = new Supplier();
        supplier.setCompanyName(name);
        supplier.setContactPerson(contact);
        supplier.setEmail(email);
        supplier.setPhone(phone);
        supplier.setAddress("Industrial Area, Demo City");
        supplier.setActive(true);
        supplier.setCreatedAt(Dates.now());
        return suppliers.insert(connection, supplier);
    }

    private User insertUser(Connection connection, String username, String fullName, Role role, String password)
            throws Exception {
        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(Dates.now());
        user.setPasswordHash(hasher.hash(password.toCharArray()));
        long id = users.insert(connection, user);
        user.setId(id);
        return user;
    }

    private void addItem(Connection connection, long orderId, long productId, int qty, long cost) throws Exception {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrderId(orderId);
        item.setProductId(productId);
        item.setQuantityOrdered(qty);
        item.setQuantityReceived(0);
        item.setUnitCostCents(cost);
        orders.insertItem(connection, item);
    }

    private String past(int daysAgo) {
        return LocalDateTime.now().minusDays(daysAgo).format(Dates.DATE_TIME);
    }
}
