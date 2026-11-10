package com.intellistock;

import com.intellistock.analytics.DemandAnalytics;
import com.intellistock.config.AppContext;
import com.intellistock.config.Database;
import com.intellistock.model.Product;
import com.intellistock.model.ProductRegistrationRequest;
import com.intellistock.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandAnalyticsTest {
    @TempDir
    Path tempDir;

    private AppContext context;
    private User admin;
    private Product fastItem;
    private Product slowItem;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test-demand.db");
        Database db = new Database(dbPath);
        context = AppContext.forTesting(db);
        admin = TestHelper.createAdmin(context, "demandadmin");

        ProductRegistrationRequest req1 = new ProductRegistrationRequest();
        req1.setName("Fast Moving Item");
        req1.setSku("FAST-001");
        req1.setNewCategoryName("Fast Track");
        req1.setUnit("units");
        req1.setInitialQuantity(100);
        req1.setReorderThreshold(30);
        req1.setCriticalThreshold(10);
        req1.setCostPriceCents(1000);
        req1.setSellingPriceCents(1500);
        fastItem = context.products().register(admin, req1);

        ProductRegistrationRequest req2 = new ProductRegistrationRequest();
        req2.setName("Slow Moving Item");
        req2.setSku("SLOW-001");
        req2.setNewCategoryName("Slow Track");
        req2.setUnit("units");
        req2.setInitialQuantity(50);
        req2.setReorderThreshold(10);
        req2.setCriticalThreshold(3);
        req2.setCostPriceCents(2000);
        req2.setSellingPriceCents(3000);
        slowItem = context.products().register(admin, req2);
    }

    @Test
    void testDemandVelocityAndReorderProjection() {
        // Dispatch 30 units of fastItem (over 14 days, daily average is 30 / 14 = 2.14 -> FAST velocity)
        context.inventory().dispatch(admin, fastItem.getId(), 30, "Customer orders", "ORD-1");

        // Dispatch 2 units of slowItem (2 / 14 = 0.14 -> SLOW velocity)
        context.inventory().dispatch(admin, slowItem.getId(), 2, "Customer orders", "ORD-2");

        List<DemandAnalytics.ProductDemand> results = context.demand().analyze();
        assertNotNull(results);
        assertEquals(2, results.size());

        DemandAnalytics.ProductDemand fastDemand = results.stream()
                .filter(d -> d.product.getId() == fastItem.getId())
                .findFirst().orElseThrow();
        assertEquals(30, fastDemand.recentOutbound);
        assertEquals("FAST", fastDemand.velocity);
        assertTrue(fastDemand.recentDailyAverage > 2.0);

        DemandAnalytics.ProductDemand slowDemand = results.stream()
                .filter(d -> d.product.getId() == slowItem.getId())
                .findFirst().orElseThrow();
        assertEquals(2, slowDemand.recentOutbound);
        assertEquals("SLOW", slowDemand.velocity);
    }
}
