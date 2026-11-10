package com.intellistock;

import com.intellistock.config.AppContext;
import com.intellistock.config.Database;
import com.intellistock.model.AlertSeverity;
import com.intellistock.model.AlertStatus;
import com.intellistock.model.AlertType;
import com.intellistock.model.InventoryAlert;
import com.intellistock.model.Product;
import com.intellistock.model.ProductRegistrationRequest;
import com.intellistock.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertEngineTest {
    @TempDir
    Path tempDir;

    private AppContext context;
    private User admin;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test-alerts.db");
        Database db = new Database(dbPath);
        context = AppContext.forTesting(db);
        admin = TestHelper.createAdmin(context, "alertadmin");
    }

    @Test
    void testLowAndCriticalStockAlertGeneration() {
        ProductRegistrationRequest req = new ProductRegistrationRequest();
        req.setName("Printer Cartridge");
        req.setSku("INK-001");
        req.setNewCategoryName("Office Supplies");
        req.setUnit("cartridges");
        req.setInitialQuantity(15); // Reorder threshold is 10, Critical is 3
        req.setReorderThreshold(10);
        req.setCriticalThreshold(3);
        req.setCostPriceCents(5000);
        req.setSellingPriceCents(7500);

        Product product = context.products().register(admin, req);

        // Initially normal stock (qty = 15 > 10) -> no low stock alert
        List<InventoryAlert> open = context.alerts().openAlerts();
        assertTrue(open.stream().noneMatch(a -> a.getProductId() != null && a.getProductId() == product.getId()));

        // Dispatch 7 items -> new qty = 8 (<= 10 and > 3) -> LOW_STOCK alert should be generated!
        context.inventory().dispatch(admin, product.getId(), 7, "Regular usage", "REF-1");

        open = context.alerts().openAlerts();
        InventoryAlert lowAlert = open.stream()
                .filter(a -> a.getProductId() != null && a.getProductId() == product.getId() && a.getAlertType() == AlertType.LOW_STOCK)
                .findFirst().orElse(null);
        assertTrue(lowAlert != null, "Low stock alert should be triggered");
        assertEquals(AlertSeverity.WARNING, lowAlert.getSeverity());
        assertEquals(AlertStatus.ACTIVE, lowAlert.getStatus());

        // Dispatch 6 more items -> new qty = 2 (<= 3) -> CRITICAL_STOCK alert should be triggered!
        context.inventory().dispatch(admin, product.getId(), 6, "Heavy usage", "REF-2");

        open = context.alerts().openAlerts();
        InventoryAlert critAlert = open.stream()
                .filter(a -> a.getProductId() != null && a.getProductId() == product.getId() && a.getAlertType() == AlertType.CRITICAL_STOCK)
                .findFirst().orElse(null);
        assertTrue(critAlert != null, "Critical stock alert should be triggered");
        assertEquals(AlertSeverity.CRITICAL, critAlert.getSeverity());

        // Receive 20 items -> new qty = 22 -> previous alerts should be automatically resolved!
        context.inventory().receive(admin, product.getId(), 20, "Batch restock", "PO-REC");

        open = context.alerts().openAlerts();
        assertFalse(open.stream().anyMatch(a -> a.getProductId() != null && a.getProductId() == product.getId()
                && (a.getAlertType() == AlertType.LOW_STOCK || a.getAlertType() == AlertType.CRITICAL_STOCK)));
    }

    @Test
    void testAcknowledgeAndResolveAlert() {
        ProductRegistrationRequest req = new ProductRegistrationRequest();
        req.setName("Out of Stock Item");
        req.setSku("OUT-001");
        req.setNewCategoryName("General");
        req.setUnit("pieces");
        req.setInitialQuantity(0);
        req.setReorderThreshold(5);
        req.setCriticalThreshold(2);
        req.setCostPriceCents(500);
        req.setSellingPriceCents(1000);

        Product p = context.products().register(admin, req);

        List<InventoryAlert> open = context.alerts().openAlerts();
        InventoryAlert alert = open.stream()
                .filter(a -> a.getProductId() != null && a.getProductId() == p.getId())
                .findFirst().orElseThrow();

        assertEquals(AlertStatus.ACTIVE, alert.getStatus());

        // Acknowledge
        context.alerts().acknowledge(alert.getId());
        List<InventoryAlert> all = context.alerts().allAlerts();
        InventoryAlert acked = all.stream().filter(a -> a.getId() == alert.getId()).findFirst().orElseThrow();
        assertEquals(AlertStatus.ACKNOWLEDGED, acked.getStatus());

        // Resolve
        context.alerts().resolve(alert.getId());
        all = context.alerts().allAlerts();
        InventoryAlert resolved = all.stream().filter(a -> a.getId() == alert.getId()).findFirst().orElseThrow();
        assertEquals(AlertStatus.RESOLVED, resolved.getStatus());
    }
}
