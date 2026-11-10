package com.intellistock;

import com.intellistock.config.AppContext;
import com.intellistock.config.Database;
import com.intellistock.model.MovementType;
import com.intellistock.model.OrderStatus;
import com.intellistock.model.Product;
import com.intellistock.model.ProductRegistrationRequest;
import com.intellistock.model.PurchaseOrder;
import com.intellistock.model.PurchaseOrderItem;
import com.intellistock.model.StockMovement;
import com.intellistock.model.Supplier;
import com.intellistock.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseOrderServiceTest {
    @TempDir
    Path tempDir;

    private AppContext context;
    private User admin;
    private Supplier supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test-po.db");
        Database db = new Database(dbPath);
        context = AppContext.forTesting(db);
        admin = TestHelper.createAdmin(context, "poadmin");

        Supplier s = new Supplier();
        s.setCompanyName("Acme Wholesale Supply");
        s.setEmail("sales@acme.example");
        s.setPhone("555-1234");
        supplier = context.suppliers().save(admin, s);

        ProductRegistrationRequest req = new ProductRegistrationRequest();
        req.setName("Industrial Tape 50m");
        req.setSku("TAPE-050");
        req.setNewCategoryName("Packaging");
        req.setUnit("rolls");
        req.setInitialQuantity(10);
        req.setReorderThreshold(20);
        req.setCriticalThreshold(5);
        req.setCostPriceCents(500);
        req.setSellingPriceCents(850);
        req.setPreferredSupplierId(supplier.getId());

        product = context.products().register(admin, req);
    }

    @Test
    void testPurchaseOrderFullLifecycle() {
        // 1. Create Draft Order
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplierId(supplier.getId());
        po.setExpectedDeliveryDate("2026-11-15");
        po.setNotes("Urgent restock batch");

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setProductId(product.getId());
        item.setQuantityOrdered(100);
        item.setUnitCostCents(450);
        po.setItems(List.of(item));

        PurchaseOrder draft = context.purchaseOrders().saveDraft(admin, po);
        assertEquals(OrderStatus.DRAFT, draft.getStatus());
        assertEquals(45000, draft.totalCents()); // 100 * 450 = 45000 cents ($450.00)

        // 2. Submit Order (DRAFT -> PENDING)
        PurchaseOrder submitted = context.purchaseOrders().submit(admin, draft.getId());
        assertEquals(OrderStatus.PENDING, submitted.getStatus());

        // 3. Partial Goods Receipt (Receive 40 out of 100)
        PurchaseOrder partial = context.purchaseOrders().receive(admin, submitted.getId(),
                Map.of(product.getId(), 40), "RECEIPT-PARTIAL-1");
        assertEquals(OrderStatus.PARTIALLY_RECEIVED, partial.getStatus());

        // Verify product inventory increased by 40 (10 initial + 40 received = 50)
        Product pAfterPartial = context.products().get(product.getId());
        assertEquals(50, pAfterPartial.getQuantity());

        // Verify stock movement log of type PO_RECEIPT was generated (ordered DESC, so index 0 is newest)
        List<StockMovement> movements = context.inventory().history(product.getId());
        StockMovement mostRecent = movements.get(0);
        assertEquals(MovementType.PO_RECEIPT, mostRecent.getMovementType());
        assertEquals(40, mostRecent.getQuantity());

        // 4. Remaining Goods Receipt (Receive remaining 60)
        PurchaseOrder full = context.purchaseOrders().receive(admin, submitted.getId(),
                Map.of(product.getId(), 60), "RECEIPT-FINAL-2");
        assertEquals(OrderStatus.RECEIVED, full.getStatus());

        Product pFinal = context.products().get(product.getId());
        assertEquals(110, pFinal.getQuantity()); // 50 + 60 = 110
    }

    @Test
    void testCannotReceiveMoreThanOrdered() {
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplierId(supplier.getId());
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setProductId(product.getId());
        item.setQuantityOrdered(30);
        item.setUnitCostCents(500);
        po.setItems(List.of(item));

        PurchaseOrder draft = context.purchaseOrders().saveDraft(admin, po);
        context.purchaseOrders().submit(admin, draft.getId());

        // Try to receive 31 when 30 was ordered
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                context.purchaseOrders().receive(admin, draft.getId(), Map.of(product.getId(), 31), null));

        assertTrue(ex.getMessage().contains("Cannot receive more than outstanding"));
    }
}
