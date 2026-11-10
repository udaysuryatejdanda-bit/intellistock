# IntelliStock AI — Example Operational Workflows

This document outlines standard end-to-end operational workflows in IntelliStock AI, detailing user actions, system logic, database mutations, and expected outcomes.

---

## Workflow 1: Registering a New Product with Opening Stock

### Scenario
A warehouse receives a new catalog item: "Ergonomic Office Chair".

### Step-by-Step Procedure
1. Log in to IntelliStock AI with `admin` or `staff` credentials.
2. Select **Register Product** from the sidebar navigation.
3. Fill in the product details:
   - **Product Name:** `Ergonomic Office Chair`
   - **SKU:** `FUR-CHR-100`
   - **Category:** Select existing or enter `Office Furniture` in *Or Create New Category*.
   - **Unit:** `pieces`
   - **Initial Quantity:** `15`
   - **Reorder Threshold:** `8`
   - **Critical Threshold:** `3`
   - **Cost Price ($):** `85.00`
   - **Selling Price ($):** `140.00`
   - **Warehouse Location:** `WH-C`
   - **Rack / Shelf:** `R2-S1`
4. Click **Register Product**.

### System Processing & Database Transactions
1. System validates that required fields are present, thresholds are non-negative, and critical threshold (3) <= reorder threshold (8).
2. Within an atomic database transaction:
   - Confirms `FUR-CHR-100` does not exist in `products`.
   - Resolves or inserts category `Office Furniture` into `categories`.
   - Inserts product row with `quantity = 0`.
   - Calls `inventory.apply(...)` to insert a movement record of type `INITIAL_STOCK` with delta = +15, previous = 0, resulting = 15.
   - Updates `products.quantity = 15`.
   - Inserts audit log record `PRODUCT_REGISTER`.
   - Commits transaction.
3. Triggers `AlertEngine.refresh()`. Stock (15) > reorder (8), so status is `NORMAL`.

---

## Workflow 2: Outbound Dispatch & Automated Low-Stock Alert

### Scenario
A customer orders 10 units of "Ergonomic Office Chair", bringing stock from 15 down to 5 (which is below the reorder threshold of 8).

### Step-by-Step Procedure
1. Navigate to **Stock Movements**.
2. Under *Record New Movement*:
   - **Select Product:** Choose `FUR-CHR-100 — Ergonomic Office Chair`. The form displays: `Current Stock: 15 pieces`.
   - **Movement Type:** Select `DISPATCH`.
   - **Quantity:** Enter `10`.
   - **Reference:** Enter customer sales order `SO-9021`.
   - **Reason:** Enter `Customer sales order fulfillment`.
3. Click **Record Movement**.

### System Processing & Database Transactions
1. System verifies requested quantity (10) <= current available stock (15).
2. Inside an atomic database transaction:
   - Updates `products.quantity = 5`.
   - Inserts `stock_movements` record: `movement_type = 'DISPATCH'`, `quantity_delta = -10`, `previous_quantity = 15`, `resulting_quantity = 5`.
   - Inserts audit log record `STOCK_DISPATCH`.
   - Commits transaction.
3. Form displays success message with new balance: `5 pieces`.
4. `AlertEngine.refresh()` detects that 5 <= 8 (reorder threshold) and 5 > 3 (critical threshold).
5. System inserts/updates an active `LOW_STOCK` alert with severity `WARNING` into `alerts`.
6. Navigate to **Alerts** page to view the generated alert.

---

## Workflow 3: Negative Stock Prevention (Safety Boundary)

### Scenario
An operator mistakenly enters a dispatch of 8 units when only 5 units are available.

### Step-by-Step Procedure
1. Navigate to **Stock Movements**.
2. Select `FUR-CHR-100` (Stock: 5 pieces).
3. Select `DISPATCH`, enter quantity `8`, and click **Record Movement**.

### Expected Outcome
1. System checks: `resulting = 5 - 8 = -3 < 0`.
2. Operation is immediately aborted.
3. An error dialog appears:  
   `"Insufficient stock. Available: 5, requested: 8"`
4. Database transaction is rolled back; no stock movement is logged, and product quantity remains untouched at `5`.

---

## Workflow 4: Procurement & Partial Goods Receipt

### Scenario
Stock is low. The purchasing manager creates a Purchase Order for 50 chairs from supplier "Northwind Wholesale". When the shipment arrives, only 30 units are delivered in the first batch.

### Step-by-Step Procedure

#### Part A: Create & Submit Purchase Order
1. Navigate to **Purchase Orders**.
2. Click **+ New Order**.
3. In the dialog:
   - **Supplier:** Select `Northwind Wholesale`.
   - **Expected Delivery Date:** Enter target date (e.g. 7 days ahead).
   - Click **+ Add Line Item** -> select `FUR-CHR-100`, enter quantity `50`, cost `$80.00` -> click **Add**.
   - Click **Save Draft Order**.
4. Order appears in table with status `DRAFT`.
5. Select the draft order and click **Submit (Draft -> Pending)**.
6. Status updates to `PENDING`.

#### Part B: Receive Partial Delivery (30 units)
1. Select the pending order and click **Receive Goods**.
2. Dialog shows: Ordered = 50, Received = 0, Outstanding = 50.
3. In *Receive Now*, enter `30`.
4. Click **Confirm Goods Receipt**.

### System Processing
1. Verifies 30 <= outstanding (50).
2. Inside an atomic transaction:
   - Inserts record into `purchase_receipts` with unique idempotency key.
   - Inserts receipt line into `purchase_receipt_items`.
   - Updates `purchase_order_items.quantity_received = 30`.
   - Updates `products.quantity = 5 + 30 = 35`.
   - Inserts `PO_RECEIPT` record into `stock_movements`.
   - Sets purchase order status to `PARTIALLY_RECEIVED`.
   - Commits transaction.
3. Alert engine recalculates: Stock is now 35 > 8. The previous `LOW_STOCK` alert is automatically transitioned to `RESOLVED`!

---

## Workflow 5: Demand Analytics & Automated Reorder Review

### Scenario
The inventory controller reviews consumption velocity across all warehouse categories.

### Step-by-Step Procedure
1. Navigate to **Demand Analysis**.
2. System computes moving average daily outbound rates over the configured window (default 14 days).
3. The table displays:
   - Product name and current stock.
   - Recent outbound vs baseline historical outbound.
   - Calculated velocity: `FAST`, `MEDIUM`, `SLOW`, or `NO_MOVEMENT`.
   - High demand flag (`YES` if current velocity exceeds baseline by configured ratio, e.g. 1.5x).
   - Estimated days of stock remaining: $\frac{\text{Current Stock}}{\text{Daily Demand}}$.
   - Suggested reorder quantity based on lead time and safety thresholds.
4. Filter by *Reorder Suggestions Only* to view all items requiring procurement replenishment.
5. Click **Create Purchase Order** to transition directly to the purchasing screen.

---

## Workflow 6: Generating & Exporting Reports to CSV

### Scenario
The accounting team requests an inventory valuation report for month-end audit.

### Step-by-Step Procedure
1. Navigate to **Reports**.
2. From the *Report* dropdown, select **Inventory Valuation Report**.
3. View the live table displaying SKU, Name, Quantity, Unit Cost, and Extended Line Valuation, concluding with a `TOTAL` valuation sum.
4. Click **Export to CSV**.
5. Choose a destination folder and filename (e.g. `valuation_october_2026.csv`).
6. Click **Save**.
7. Open the exported `.csv` file in Microsoft Excel or Google Sheets. The UTF-8 BOM encoding ensures all currency symbols and characters render cleanly without character corruption.
