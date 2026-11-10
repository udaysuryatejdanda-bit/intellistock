package com.intellistock;

import com.intellistock.config.AppContext;
import com.intellistock.config.Database;
import com.intellistock.model.MovementType;
import com.intellistock.model.Product;
import com.intellistock.model.ProductRegistrationRequest;
import com.intellistock.model.StockMovement;
import com.intellistock.model.StockStatus;
import com.intellistock.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductServiceTest {
    @TempDir
    Path tempDir;

    private AppContext context;
    private User admin;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test-products.db");
        Database db = new Database(dbPath);
        context = AppContext.forTesting(db);
        admin = TestHelper.createAdmin(context, "prodadmin");
    }

    @Test
    void testRegisterProductWithInitialStock() {
        ProductRegistrationRequest req = new ProductRegistrationRequest();
        req.setName("Test Laptop Stand");
        req.setSku("STAND-001");
        req.setNewCategoryName("Office Accessories");
        req.setUnit("pieces");
        req.setInitialQuantity(25);
        req.setReorderThreshold(10);
        req.setCriticalThreshold(3);
        req.setCostPriceCents(1500);
        req.setSellingPriceCents(2500);
        req.setWarehouseLocation("WH-A");
        req.setRackShelf("R1-S1");

        Product product = context.products().register(admin, req);

        assertNotNull(product);
        assertTrue(product.getId() > 0);
        assertEquals("STAND-001", product.getSku());
        assertEquals("Test Laptop Stand", product.getName());
        assertEquals(25, product.getQuantity());
        assertEquals(StockStatus.NORMAL, product.getStockStatus());

        // Verify initial movement history was created in transaction
        List<StockMovement> history = context.inventory().history(product.getId());
        assertEquals(1, history.size());
        StockMovement init = history.get(0);
        assertEquals(MovementType.INITIAL_STOCK, init.getMovementType());
        assertEquals(25, init.getQuantity());
        assertEquals(25, init.getResultingQuantity());
        assertEquals(0, init.getPreviousQuantity());
    }

    @Test
    void testDuplicateSkuRejection() {
        ProductRegistrationRequest req1 = new ProductRegistrationRequest();
        req1.setName("Item 1");
        req1.setSku("DUP-SKU-999");
        req1.setNewCategoryName("Category 1");
        req1.setUnit("units");
        req1.setInitialQuantity(5);
        req1.setReorderThreshold(2);
        req1.setCostPriceCents(100);
        req1.setSellingPriceCents(200);
        context.products().register(admin, req1);

        ProductRegistrationRequest req2 = new ProductRegistrationRequest();
        req2.setName("Item 2 with Duplicate SKU");
        req2.setSku("DUP-SKU-999");
        req2.setNewCategoryName("Category 1");
        req2.setUnit("units");
        req2.setInitialQuantity(10);
        req2.setReorderThreshold(2);
        req2.setCostPriceCents(150);
        req2.setSellingPriceCents(300);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                context.products().register(admin, req2));
        assertTrue(ex.getMessage().contains("SKU already exists"));
    }

    @Test
    void testInvalidThresholdsValidation() {
        ProductRegistrationRequest req = new ProductRegistrationRequest();
        req.setName("Faulty Thresholds");
        req.setSku("FLT-001");
        req.setNewCategoryName("Category 1");
        req.setUnit("units");
        req.setInitialQuantity(10);
        req.setReorderThreshold(5);
        req.setCriticalThreshold(8); // Critical cannot exceed reorder!
        req.setCostPriceCents(100);
        req.setSellingPriceCents(200);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                context.products().register(admin, req));
        assertTrue(ex.getMessage().contains("Critical threshold cannot exceed"));
    }

    @Test
    void testUpdateMetadataAndArchiving() {
        ProductRegistrationRequest req = new ProductRegistrationRequest();
        req.setName("Original Name");
        req.setSku("ARCH-001");
        req.setNewCategoryName("Category A");
        req.setUnit("boxes");
        req.setInitialQuantity(10);
        req.setReorderThreshold(5);
        req.setCostPriceCents(1000);
        req.setSellingPriceCents(2000);

        Product created = context.products().register(admin, req);
        assertEquals("Original Name", created.getName());

        created.setName("Updated Name V2");
        created.setWarehouseLocation("WH-NORTH");
        Product updated = context.products().updateMetadata(admin, created);
        assertEquals("Updated Name V2", updated.getName());
        assertEquals("WH-NORTH", updated.getWarehouseLocation());
        assertEquals(10, updated.getQuantity()); // Quantity remains unchanged!

        assertFalse(updated.isArchived());
        context.products().archive(admin, updated.getId());

        Product archived = context.products().get(updated.getId());
        assertTrue(archived.isArchived());
        assertEquals(StockStatus.ARCHIVED, archived.getStockStatus());
    }
}
