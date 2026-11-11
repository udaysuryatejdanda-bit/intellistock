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
    void testFullApplicationWorkflowWithDemoDataAndPersistence() throws Exception {
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
        try {
            context.reports().exportCsv("Inventory Valuation", inventoryReport, csvPath);
            assertTrue(java.nio.file.Files.exists(csvPath));
            assertTrue(java.nio.file.Files.size(csvPath) > 50);
        } catch (Exception ex) {
            org.junit.jupiter.api.Assertions.fail("CSV export failed: " + ex.getMessage());
        }

        // 9. Test Downward and Upward Adjustments
        StockMovement downAdj = context.inventory().adjust(admin, saved.getId(), -2, "Damaged stock audit write-off");
        assertEquals(-2, downAdj.getQuantityDelta());
        assertEquals(2, downAdj.getResultingQuantity());
        assertEquals(2, context.products().get(saved.getId()).getQuantity());

        StockMovement upAdj = context.inventory().adjust(admin, saved.getId(), 5, "Physical recount surplus");
        assertEquals(5, upAdj.getQuantityDelta());
        assertEquals(7, upAdj.getResultingQuantity());
        assertEquals(7, context.products().get(saved.getId()).getQuantity());

        // 10. Test Procurement & Purchase Order lifecycle
        com.intellistock.model.Supplier newSup = new com.intellistock.model.Supplier();
        newSup.setCompanyName("ErgoTech Supplies Ltd");
        newSup.setContactPerson("David Chen");
        newSup.setEmail("david@ergotech.example");
        newSup.setPhone("555-8899");
        newSup = context.suppliers().save(admin, newSup);
        assertTrue(newSup.getId() > 0);

        com.intellistock.model.PurchaseOrder po = new com.intellistock.model.PurchaseOrder();
        po.setSupplierId(newSup.getId());
        po.setExpectedDeliveryDate(com.intellistock.util.Dates.today());
        po.setNotes("Priority restocking");

        com.intellistock.model.PurchaseOrderItem poItem = new com.intellistock.model.PurchaseOrderItem();
        poItem.setProductId(saved.getId());
        poItem.setQuantityOrdered(30);
        poItem.setUnitCostCents(11000);
        po.setItems(List.of(poItem));

        com.intellistock.model.PurchaseOrder draft = context.purchaseOrders().saveDraft(admin, po);
        assertEquals(com.intellistock.model.OrderStatus.DRAFT, draft.getStatus());

        com.intellistock.model.PurchaseOrder submitted = context.purchaseOrders().submit(admin, draft.getId());
        assertEquals(com.intellistock.model.OrderStatus.PENDING, submitted.getStatus());

        // Receive partial (10 units)
        com.intellistock.model.PurchaseOrder partial = context.purchaseOrders().receive(admin, submitted.getId(),
                java.util.Map.of(saved.getId(), 10), "GRN-PARTIAL-01");
        assertEquals(com.intellistock.model.OrderStatus.PARTIALLY_RECEIVED, partial.getStatus());
        assertEquals(17, context.products().get(saved.getId()).getQuantity()); // 7 + 10 = 17

        // Receive remainder (20 units)
        com.intellistock.model.PurchaseOrder completed = context.purchaseOrders().receive(admin, submitted.getId(),
                java.util.Map.of(saved.getId(), 20), "GRN-FINAL-02");
        assertEquals(com.intellistock.model.OrderStatus.RECEIVED, completed.getStatus());
        assertEquals(37, context.products().get(saved.getId()).getQuantity()); // 17 + 20 = 37

        // 11. Test Demand Analytics & PDF Report rendering
        List<com.intellistock.analytics.DemandAnalytics.ProductDemand> demandList = context.demand().analyze();
        assertNotNull(demandList);
        assertFalse(demandList.isEmpty());

        com.intellistock.analytics.DemandAnalytics.ProductDemand targetDemand = demandList.stream()
                .filter(d -> d.product != null && d.product.getId() == saved.getId())
                .findFirst().orElseThrow();
        assertEquals(18, targetDemand.recentOutbound); // 16 dispatch + 2 adjustment deduction = 18 total outbound

        com.intellistock.report.DemandPrintReport printReport = new com.intellistock.report.DemandPrintReport(targetDemand);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(595, 842, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.print.PageFormat pf = new java.awt.print.PageFormat();
        int pageResult = printReport.print(img.createGraphics(), pf, 0);
        assertEquals(java.awt.print.Printable.PAGE_EXISTS, pageResult);

        // 12. Test Alert Lifecycle (Acknowledge and Resolve)
        List<InventoryAlert> openList = context.alerts().openAlerts();
        assertFalse(openList.isEmpty());
        InventoryAlert testAlert = openList.get(0);
        context.alerts().acknowledge(testAlert.getId());
        assertEquals(com.intellistock.model.AlertStatus.ACKNOWLEDGED,
                context.alerts().allAlerts().stream().filter(a -> a.getId() == testAlert.getId()).findFirst().orElseThrow().getStatus());
        context.alerts().resolve(testAlert.getId());
        assertEquals(com.intellistock.model.AlertStatus.RESOLVED,
                context.alerts().allAlerts().stream().filter(a -> a.getId() == testAlert.getId()).findFirst().orElseThrow().getStatus());

        // 13. Test Backup Service (Creation and Validation)
        Path backupFile = context.backups().createBackup(admin);
        assertTrue(java.nio.file.Files.exists(backupFile));
        context.backups().validateBackup(backupFile);

        // 14. Test Dashboard Snapshot
        com.intellistock.service.DashboardService.DashboardSnapshot snap = context.dashboard().snapshot();
        assertNotNull(snap);
        assertTrue(snap.totalProducts > 0);
        assertTrue(snap.totalQuantity > 0);
        assertTrue(snap.valuationCents > 0);
    }
}
