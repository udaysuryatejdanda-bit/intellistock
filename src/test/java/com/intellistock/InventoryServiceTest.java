package com.intellistock;

import com.intellistock.config.AppContext;
import com.intellistock.config.Database;
import com.intellistock.model.MovementType;
import com.intellistock.model.Product;
import com.intellistock.model.ProductRegistrationRequest;
import com.intellistock.model.StockMovement;
import com.intellistock.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryServiceTest {
    @TempDir
    Path tempDir;

    private AppContext context;
    private User admin;
    private User staff;
    private Product product;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test-inventory.db");
        Database db = new Database(dbPath);
        context = AppContext.forTesting(db);

        admin = TestHelper.createAdmin(context, "invadmin");
        staff = TestHelper.createStaff(context, "invstaff");

        ProductRegistrationRequest req = new ProductRegistrationRequest();
        req.setName("Wireless Keyboard");
        req.setSku("KEY-001");
        req.setNewCategoryName("Electronics");
        req.setUnit("pieces");
        req.setInitialQuantity(50);
        req.setReorderThreshold(20);
        req.setCriticalThreshold(5);
        req.setCostPriceCents(3000);
        req.setSellingPriceCents(4500);

        product = context.products().register(admin, req);
    }

    @Test
    void testStockReceipt() {
        StockMovement receipt = context.inventory().receive(staff, product.getId(), 25, "Restock batch A", "PO-101");

        assertEquals(MovementType.RECEIPT, receipt.getMovementType());
        assertEquals(25, receipt.getQuantity());
        assertEquals(25, receipt.getQuantityDelta());
        assertEquals(50, receipt.getPreviousQuantity());
        assertEquals(75, receipt.getResultingQuantity());

        Product refreshed = context.products().get(product.getId());
        assertEquals(75, refreshed.getQuantity());
    }

    @Test
    void testStockDispatch() {
        StockMovement dispatch = context.inventory().dispatch(staff, product.getId(), 15, "Customer sale", "INV-5001");

        assertEquals(MovementType.DISPATCH, dispatch.getMovementType());
        assertEquals(15, dispatch.getQuantity());
        assertEquals(-15, dispatch.getQuantityDelta());
        assertEquals(50, dispatch.getPreviousQuantity());
        assertEquals(35, dispatch.getResultingQuantity());

        Product refreshed = context.products().get(product.getId());
        assertEquals(35, refreshed.getQuantity());
    }

    @Test
    void testNegativeStockPrevention() {
        // Current quantity is 50. Attempting to dispatch 51 must fail.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                context.inventory().dispatch(staff, product.getId(), 51, "Excess dispatch", "INV-ERR"));

        assertTrue(ex.getMessage().contains("Insufficient stock"));

        // Verify balance remained at 50 (transaction rollback)
        Product refreshed = context.products().get(product.getId());
        assertEquals(50, refreshed.getQuantity());

        List<StockMovement> movements = context.inventory().history(product.getId());
        assertEquals(1, movements.size()); // Only opening stock movement exists
    }

    @Test
    void testDamagedStockRequiresReason() {
        // Empty reason should fail
        assertThrows(IllegalArgumentException.class, () ->
                context.inventory().damaged(staff, product.getId(), 2, ""));

        // Valid damaged stock
        StockMovement dmg = context.inventory().damaged(staff, product.getId(), 2, "Water damaged during transit");
        assertEquals(MovementType.DAMAGED, dmg.getMovementType());
        assertEquals(-2, dmg.getQuantityDelta());
        assertEquals(48, dmg.getResultingQuantity());
    }

    @Test
    void testAdjustmentSecurityAndExecution() {
        // Staff should not be allowed to adjust stock
        assertThrows(SecurityException.class, () ->
                context.inventory().adjust(staff, product.getId(), -5, "Staff adjustment"));

        // Admin can perform inventory adjustments
        StockMovement adj = context.inventory().adjust(admin, product.getId(), -10, "Cycle count discrepancy");
        assertEquals(MovementType.ADJUSTMENT, adj.getMovementType());
        assertEquals(-10, adj.getQuantityDelta());
        assertEquals(40, adj.getResultingQuantity());

        Product refreshed = context.products().get(product.getId());
        assertEquals(40, refreshed.getQuantity());
    }
}
