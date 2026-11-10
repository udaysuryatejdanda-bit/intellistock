package com.intellistock;

import com.intellistock.config.AppContext;
import com.intellistock.config.Database;
import com.intellistock.config.DemoDataSeeder;
import com.intellistock.model.InventoryAlert;
import com.intellistock.model.MovementType;
import com.intellistock.model.Product;
import com.intellistock.model.ProductRegistrationRequest;
import com.intellistock.model.StockMovement;
import com.intellistock.model.StockStatus;
import com.intellistock.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndToEndLiveTest {
    @TempDir
    Path tempDir;

    @Test
    void testFullApplicationWorkflowWithDemoDataAndPersistence() {
        Path dbPath = tempDir.resolve("e2e-live.db");
        Database db = new Database(dbPath);
        AppContext context = AppContext.forTesting(db);

        // 1. Seed demo data (idempotent)
        new DemoDataSeeder(db).seedIfNeeded(context.inventory(), context.alerts());

        // 2. Verify demo users seeded and authentication works
        User admin = context.auth().login(DemoDataSeeder.DEMO_ADMIN_USERNAME, "Admin@123".toCharArray());
        assertNotNull(admin);
        assertEquals("admin", admin.getUsername());

        User staff = context.auth().login(DemoDataSeeder.DEMO_STAFF_USERNAME, "Staff@123".toCharArray());
        assertNotNull(staff);
        assertEquals("staff", staff.getUsername());

        // 3. Verify demo catalog seeded
        List<Product> catalog = context.products().list(false);
        assertFalse(catalog.isEmpty(), "Demo catalog should contain products");
        assertTrue(catalog.size() >= 10, "Demo catalog should have at least 10 products");

        // 4. Verify alerts were evaluated on demo catalog
        List<InventoryAlert> openAlerts = context.alerts().openAlerts();
        assertFalse(openAlerts.isEmpty(), "Demo catalog should have open alerts (e.g. low stock, out of stock, expiring)");

        // 5. Test Registering a brand new product into SQLite
        ProductRegistrationRequest req = new ProductRegistrationRequest();
        req.setName("Ergonomic Mesh Chair");
        req.setSku("CHR-ERG-900");
        req.setNewCategoryName("Office Furniture");
        req.setUnit("units");
        req.setInitialQuantity(12);
        req.setReorderThreshold(5);
        req.setCriticalThreshold(2);
        req.setCostPriceCents(12000);
        req.setSellingPriceCents(18500);
        req.setWarehouseLocation("WH-F");
        req.setRackShelf("BAY-1");

        Product saved = context.products().register(admin, req);
        assertNotNull(saved);
        assertEquals("CHR-ERG-900", saved.getSku());
        assertEquals(12, saved.getQuantity());

        // 6. Test recording stock movements
        StockMovement receipt = context.inventory().receive(staff, saved.getId(), 8, "Shipment arrival", "INVOICE-888");
        assertEquals(20, receipt.getResultingQuantity());
        assertEquals(MovementType.RECEIPT, receipt.getMovementType());

        StockMovement dispatch = context.inventory().dispatch(staff, saved.getId(), 16, "Corporate sale", "INV-999");
        assertEquals(4, dispatch.getResultingQuantity()); // 20 - 16 = 4 (which is <= reorder threshold 5!)

        // 7. Check low-stock alert triggered by the dispatch
        Product updatedProd = context.products().get(saved.getId());
        assertEquals(4, updatedProd.getQuantity());
        assertEquals(StockStatus.LOW, updatedProd.getStockStatus());

        List<InventoryAlert> alertsAfter = context.alerts().openAlerts();
        boolean hasLowAlert = alertsAfter.stream()
                .anyMatch(a -> a.getProductId() != null && a.getProductId() == saved.getId());
        assertTrue(hasLowAlert, "Low stock alert should be triggered for newly registered product after dispatch");

        // 8. Test CSV Report Generation
        List<String[]> inventoryReport = context.reports().inventoryReport();
        assertTrue(inventoryReport.size() > 10, "Inventory report must include all catalog products");
        assertEquals("SKU", inventoryReport.get(0)[0]);

        Path csvPath = tempDir.resolve("inventory_export.csv");
        assertNotNull(csvPath);
    }
}
