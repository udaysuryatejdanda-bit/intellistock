# College Mini-Project Report: IntelliStock AI

**Project Title:** IntelliStock AI — Intelligent Inventory & Stock Management System  
**Academic Year:** 2026–2027  
**Technology Stack:** Java 21, Java Swing GUI, JDBC, SQLite, Maven, JUnit 5  

---

## Abstract

Inventory management is a critical business function for warehouses, wholesalers, and retail distributors. Manual record-keeping or disconnected spreadsheet tracking frequently causes stock discrepancies, undetected shortages, delayed procurement, and inventory depreciation through expiration. 

**IntelliStock AI** is a robust, desktop-based inventory and warehouse management system developed in Java 21 using Java Swing and SQLite via Java Database Connectivity (JDBC). The application follows an object-oriented 3-tier architecture separating presentation, service logic, and relational persistence. Key capabilities include atomic product registration, real-time catalog search and filtering, multi-type stock movements (receipts, dispatches, damages, returns, adjustments) with strict non-negative quantity guarantees, automated rule-based stock alerts (out-of-stock, critical stock, low stock, product expiry), and moving-average outbound demand analytics for suggested reorder calculations. A procurement pipeline supports draft creation, submission, and partial goods receipt. Role-based access control (RBAC) with PBKDF2 cryptographic password hashing protects administrative functions, and an online database backup and disaster recovery module ensures high data availability. Comprehensive automated testing with JUnit 5 confirms 100% compliance across all core business rules and database constraints.

---

## 1. Introduction & Problem Statement

### 1.1 Problem Statement
In physical inventory management, businesses encounter several major operational hurdles:
1. **Stock Inaccuracies:** Without atomic transactions, concurrent sales and warehouse receipts cause inventory counts to become mismatched or turn negative.
2. **Stockouts and Critical Depletions:** Businesses lack proactive notification systems when stock falls below replenishment levels, leading to lost customer orders.
3. **Disorganized Procurement:** Purchasing orders are often coordinated via phone or email without tracking partial deliveries, creating confusion when shipments arrive in installments.
4. **Spoilage and Expiration Loss:** Perishable items with expiration dates are overlooked until after they expire, causing financial write-offs.
5. **Opaque Analytics Claims:** Many modern commercial solutions claim artificial intelligence or predictive machine learning when basic statistical run rates or moving averages would be significantly more transparent, reliable, and auditable.

### 1.2 Purpose of IntelliStock AI
IntelliStock AI provides a standalone, reliable desktop solution that enforces relational database constraints, guarantees transaction atomicity, automates alert notifications, computes transparent moving-average demand analytics, and tracks every inventory movement with full audit logging. It is strictly an enterprise warehouse inventory management system, **not** a financial stock-market trading tool.

---

## 2. Project Objectives

- **Develop a Pure Java Desktop Application:** Utilize Java 21 and Java Swing for a responsive, cross-platform graphical user interface.
- **Relational Persistence via JDBC:** Use SQLite and native JDBC prepared statements with try-with-resources to prevent SQL injection and resource leaks.
- **Transactional Consistency:** Wrap multi-entity mutations (e.g. product registration with initial stock, stock dispatches, partial goods receipts) inside explicit database transactions (`commit`/`rollback`).
- **Enforce Business Constraints:** Prevent negative stock balances at both the UI and database schema level (`CHECK (quantity >= 0)`).
- **Smart Rule-Based Alert Engine:** Dynamically calculate inventory alerts based on real-time quantities, thresholds, and expiration dates.
- **Transparent Run-Rate Analytics:** Implement a moving-average outbound velocity model comparing recent demand against baseline history to calculate days of supply and reorder suggestions without unverified AI/ML claims.
- **Security & Auditing:** Secure user credentials using PBKDF2 with HMAC-SHA256 and log all significant operations in an immutable audit ledger.

---

## 3. Existing System vs. Proposed System

| Feature | Existing Spreadsheet / Legacy System | Proposed IntelliStock AI System |
| :--- | :--- | :--- |
| **Data Storage** | Disconnected Excel/CSV files or flat files | Centralized ACID-compliant SQLite relational database |
| **Data Integrity** | Prone to accidental overwrites and negative stock | Enforced via primary/foreign keys, `CHECK` constraints, and transactions |
| **Transaction Safety** | Non-existent; crashes leave partial data | Atomic transactions (`commit`/`rollback`) guarantee all-or-nothing execution |
| **Alert Notifications** | Manual inspection required | Real-time automated alerts for out-of-stock, critical, low, and expired goods |
| **Procurement** | Disconnected email/paper purchase orders | Integrated PO management supporting draft, pending, and partial delivery receipts |
| **Demand Analytics** | Manual guesswork | Automated moving-average velocity analysis and reorder recommendations |
| **User Access Control** | Single shared file or unprotected sheets | Role-based security (`ADMIN` vs. `STAFF`) with PBKDF2 password encryption |
| **Audit Trail** | Untracked edits | Complete chronological audit log recording actor, action, timestamp, and details |

---

## 4. System Architecture

IntelliStock AI is designed around a classic **3-Tier Layered Architecture**:

```mermaid
graph TD
    subgraph Presentation Layer
        A[Java Swing GUI - MainFrame]
        A --> B[DashboardPage]
        A --> C[RegisterProductPage]
        A --> D[ProductsPage]
        A --> E[StockMovementsPage]
        A --> F[AlertsPage]
        A --> G[SuppliersPage]
        A --> H[PurchaseOrdersPage]
        A --> I[DemandPage]
        A --> J[ReportsPage]
        A --> K[UsersPage]
        A --> L[SettingsPage]
    end

    subgraph Service Layer (Business Logic)
        M[ProductService]
        N[InventoryService]
        O[AlertEngine]
        P[DemandAnalytics]
        Q[PurchaseOrderService]
        R[AuthService]
        S[ReportService]
        T[BackupService]
    end

    subgraph Data Access Layer (JDBC Repositories)
        U[ProductRepository]
        V[StockMovementRepository]
        W[AlertRepository]
        X[PurchaseOrderRepository]
        Y[SupplierRepository]
        Z[UserRepository]
        AA[SettingsRepository]
        AB[AuditRepository]
    end

    subgraph Database Layer
        DB[(SQLite Relational Database)]
    end

    Presentation Layer --> Service Layer
    Service Layer --> Data Access Layer
    Data Access Layer --> DB
```

1. **Presentation Layer (Java Swing):** Manages user interactions via intuitive forms, tables, metric cards, status badges, and bar charts. Multi-threading with `SwingWorker` keeps the UI responsive during database tasks.
2. **Service Layer (Business Logic):** Enforces business rules, permission checks, stock calculation logic, alert rule evaluation, demand moving averages, and transaction orchestration.
3. **Data Access Layer (JDBC Repositories):** Encapsulates raw SQL queries using `PreparedStatement` parameters and maps `ResultSet` rows to domain model objects.
4. **Database Layer (SQLite):** Executes SQLite relational storage with WAL mode, foreign key enforcement, and check constraints.

---

## 5. Functional Modules

1. **Authentication & Authorization (`AuthService`, `LoginFrame`):**
   - Validates user credentials using PBKDF2 password verification.
   - Enforces role separation (`ADMIN` vs. `STAFF`).
2. **Product Registration & Catalog Management (`ProductService`, `RegisterProductPage`, `ProductsPage`):**
   - Registers new items with unique SKU validation and category resolution.
   - Updates item metadata (name, price, thresholds, storage location).
   - Allows archiving discontinued items while preserving historical data.
3. **Stock Movement Operations (`InventoryService`, `StockMovementsPage`):**
   - Records receipts (+), customer dispatches (-), returns (+), damages (-), and adjustments (+/-).
   - Enforces non-negative stock invariants.
4. **Smart Stock Alert Engine (`AlertEngine`, `AlertsPage`):**
   - Continuously evaluates live quantities against thresholds and dates.
   - Automatically generates, updates, and resolves alerts for:
     - `OUT_OF_STOCK` (Critical)
     - `CRITICAL_STOCK` (Critical)
     - `LOW_STOCK` (Warning)
     - `EXPIRED` (Critical)
     - `EXPIRING` (Warning)
     - `HIGH_DEMAND` (Warning)
     - `REORDER` (Info)
5. **Demand Analytics & Inventory Run-Rate (`DemandAnalytics`, `DemandPage`):**
   - Analyzes outbound volume over a configured window (e.g. 14 days) against a historical baseline.
   - Computes daily sales velocity (`FAST`, `MEDIUM`, `SLOW`, `NO_MOVEMENT`).
   - Recommends reorder quantities using:  
     $$\text{Target} = \lceil \text{Daily Demand} \times (\text{Lead Time} + \text{Review Window}) \rceil + \text{Safety Stock}$$  
     $$\text{Suggested Reorder} = \max(0, \text{Target} - \text{Current Stock})$$
6. **Procurement & Goods Receipt (`PurchaseOrderService`, `PurchaseOrdersPage`):**
   - Creates draft purchase orders with suppliers and custom line items.
   - Submits orders and supports partial deliveries, updating inventory quantities and remaining balances.
7. **Reporting & Data Export (`ReportService`, `ReportsPage`):**
   - Formats tabular reports for valuation, movements, suppliers, and expiration dates.
   - Exports data to CSV with UTF-8 BOM encoding.
8. **Disaster Recovery & Settings (`BackupService`, `SettingsPage`):**
   - Creates online SQLite backups via `VACUUM INTO`.
   - Restores database files with pre-validation and safety backups.
   - Manages system threshold parameters.

---

## 6. Database Design

### Relational Schema Summary
- `users`: ID, username, password_hash, full_name, role, active, created_at.
- `categories`: ID, name.
- `suppliers`: ID, company_name, contact_person, email, phone, address, active.
- `products`: ID, sku, name, category_id, unit, barcode, quantity, reorder_threshold, critical_threshold, location, cost_price_cents, selling_price_cents, supplier_id, archived.
- `stock_movements`: ID, product_id, movement_type, quantity, quantity_delta, previous_quantity, resulting_quantity, reason, reference, user_id, occurred_at.
- `purchase_orders`: ID, supplier_id, status, expected_delivery_date, created_by, created_at.
- `purchase_order_items`: ID, purchase_order_id, product_id, quantity_ordered, quantity_received, unit_cost_cents.
- `purchase_receipts` & `purchase_receipt_items`: Receipt ledger with idempotency keys.
- `alerts`: ID, product_id, purchase_order_id, alert_type, severity, message, status, created_at.
- `audit_logs`: ID, user_id, action, entity_type, entity_id, details, created_at.
- `app_settings`: key, value.

---

## 7. Object-Oriented Programming (OOP) Principles Applied

- **Encapsulation:** Domain models (`Product`, `StockMovement`, `PurchaseOrder`, `User`) protect internal state through private fields and public accessors/mutators. Invariants (e.g. non-negative prices, valid thresholds) are validated before setting.
- **Abstraction:** The database interface abstracts SQL connection handling and transaction demarcation (`Database.inTransaction(SqlWork)`), shielding service logic from raw JDBC boilerplate.
- **Inheritance:** Swing components extend base classes (`MainFrame` extends `JFrame`, custom widgets extend `JPanel`, custom table models extend `DefaultTableModel`).
- **Polymorphism & Interface-Driven Design:** The `Page` interface defines `id()`, `title()`, `component()`, and `refresh()`. `MainFrame` manages all pages polymorphically via a `Map<String, Page>` inside a Swing `CardLayout`.
- **Separation of Concerns:** Each class has a single, well-defined responsibility (UI rendering, business validation, SQL execution, or security hashing).

---

## 8. Implementation Details

- **JDBC Prepared Statements:** Every query uses parameterized `PreparedStatement` calls, eliminating SQL injection vulnerabilities.
- **Database Transactions:** ACID compliance is maintained using `connection.setAutoCommit(false)`, `commit()`, and `rollback()` in a structured try-with-resources block.
- **Monetary Cent Representation:** Currency is stored as integer cents (`cost_price_cents`) rather than floating-point numbers, preventing binary rounding anomalies.
- **Security:** Password storage uses PBKDF2 with HMAC-SHA256, 65,536 iterations, and a secure random 16-byte salt. Sensitive password memory is cleared (`Arrays.fill(password, '\0')`) immediately after verification.

---

## 9. Test Results & Verification

IntelliStock AI includes a comprehensive JUnit 5 test suite with 20 automated unit and integration tests.

| Class | Tests Run | Passed | Failures | Errors | Result |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `AlertEngineTest` | 2 | 2 | 0 | 0 | **PASS** |
| `AuthServiceTest` | 5 | 5 | 0 | 0 | **PASS** |
| `DemandAnalyticsTest` | 1 | 1 | 0 | 0 | **PASS** |
| `EndToEndLiveTest` | 1 | 1 | 0 | 0 | **PASS** |
| `InventoryServiceTest` | 5 | 5 | 0 | 0 | **PASS** |
| `ProductServiceTest` | 4 | 4 | 0 | 0 | **PASS** |
| `PurchaseOrderServiceTest` | 2 | 2 | 0 | 0 | **PASS** |
| **Total** | **20** | **20** | **0** | **0** | **100% PASS** |

---

## 10. Limitations

- **Single Desktop Runtime:** While multiple background threads run via SwingWorker, SQLite is optimized for a single local application process rather than high-concurrency client-server web architectures.
- **Physical Barcode Scanners:** Hardware barcode scanner integration requires keyboard wedge emulation.
- **Rule-Based Demand Projections:** The moving average model reflects past outbound volume and does not account for external macroeconomic variables or seasonal spikes without manual threshold tuning.

---

## 11. Future Scope & Conclusion

### 11.1 Future Enhancements
1. **Barcode / QR Code Generation:** Implement automatic QR code printing directly onto adhesive warehouse shelf labels.
2. **Network Database Backend:** Provide an optional PostgreSQL or MySQL connection adapter for multi-workstation warehouse networks.
3. **Automated Supplier Emailing:** Integrate JavaMail to dispatch PDF purchase orders directly to suppliers upon order submission.
4. **Predictive Seasonal Forecasts:** Incorporate Holt-Winters exponential smoothing for seasonal retail inventory spikes.

### 11.2 Conclusion
IntelliStock AI successfully delivers a robust, reliable, and user-friendly inventory management platform. By leveraging Java 21, Java Swing, JDBC, SQLite transactions, and clean object-oriented architecture, the system provides full transparency, data consistency, and operational safety for physical warehouse operations.
