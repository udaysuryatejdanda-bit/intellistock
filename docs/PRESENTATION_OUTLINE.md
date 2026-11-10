# IntelliStock AI — Presentation Outline & Viva Guide

## 1. Presentation Slide Structure

### Slide 1: Title & Project Overview
- **Title:** IntelliStock AI — Intelligent Inventory & Stock Management System
- **Domain:** Enterprise Desktop Systems & Warehouse Automation
- **Technology Stack:** Java 21, Java Swing, JDBC, SQLite, Maven, JUnit 5
- **Key Message:** High-reliability physical inventory management with ACID transaction safety and smart stock alerts (not a stock-market app).

### Slide 2: Problem Statement & Motivation
- **Manual Errors:** Spreadsheets lead to duplicate entries, accidental cell overwrites, and negative stock.
- **Stockout Risks:** Lack of automated replenishment alerts leads to delayed fulfillment.
- **Lost Revenue through Expiration:** Perishable goods expire unnoticed on warehouse shelves.
- **Audit Deficits:** Inability to track who performed receipts, dispatches, or adjustments.

### Slide 3: System Architecture & Technologies
- **3-Tier Layered Architecture:**
  1. *Presentation Layer:* Java Swing (native look & feel, `CardLayout`, custom components).
  2. *Service Layer:* Business rules, permission checks, moving averages, alert engine.
  3. *Data Layer:* JDBC prepared statements, SQLite relational database, ACID transactions.
- **Why SQLite + JDBC?** Embedded, zero-configuration, robust local ACID storage with zero external server dependencies.

### Slide 4: Database Design & Integrity
- **Relational Integrity:** Foreign keys enforced via `PRAGMA foreign_keys = ON`.
- **Constraint Enforcement:** `CHECK (quantity >= 0)` guarantees no negative stock.
- **Partial Unique Indexes:** Unique SKU enforcement and barcode uniqueness.
- **Currency Storage:** Non-negative integer cents prevent floating-point rounding errors.

### Slide 5: Core Feature 1 — Atomic Product Registration
- Comprehensive registration form with required field validation.
- Enforces unique SKU and auto-creates categories.
- Single atomic transaction creates product, initializes stock, and logs audit record.

### Slide 6: Core Feature 2 — Stock Movements & Negative Stock Prevention
- Unified movement recording: Receipt (+), Dispatch (-), Return (+), Damaged (-), Adjustment (+/-).
- Business rule enforcement: Mandatory reasons for damages/adjustments; insufficient stock throws exceptions and rolls back.

### Slide 7: Core Feature 3 — Smart Stock Alert Engine
- Dynamic rule-based recalculation on all stock updates.
- Real-time categorizations: Out-of-Stock, Critical-Stock, Low-Stock, Expired, and Expiring.
- Alert lifecycle: `ACTIVE` -> `ACKNOWLEDGED` -> `RESOLVED` (auto-resolved upon restock).

### Slide 8: Core Feature 4 — Demand Analytics & Reorder Suggestions
- Moving-average outbound velocity model comparing recent demand against baseline periods.
- Velocity classification: `FAST`, `MEDIUM`, `SLOW`, `NO_MOVEMENT`.
- Run-rate projection: Days of supply remaining and suggested reorder calculation using lead time and safety stock.
- Transparent formula — honest labeling without artificial AI/ML claims.

### Slide 9: Core Feature 5 — Purchase Order Procurement
- Vendor relationship management.
- Complete PO workflow: Draft -> Submit -> Partial Receipt -> Full Receipt.
- Partial receipts automatically update inventory and decrement remaining PO balances.

### Slide 10: Security, Reporting & Disaster Recovery
- PBKDF2 with HMAC-SHA256 password hashing.
- Role-Based Access Control (`ADMIN` vs. `STAFF`).
- On-screen tabular reports with single-click UTF-8 BOM CSV exports.
- Zero-downtime database backups via SQLite `VACUUM INTO` and database restoration.

### Slide 11: Testing & Results Summary
- 20 comprehensive automated JUnit 5 tests covering 100% of core business logic.
- 0 failures, 0 errors, full regression test passing.

---

## 2. Live Demonstration Script (Step-by-Step)

1. **Launch Application:**
   - Run `.\run.cmd` or `java -jar target\intellistock-ai.jar`.
   - Point out clean native Swing startup and Login window.
2. **Staff Sign In:**
   - Log in as `staff` / `Staff@123`.
   - Note the restricted sidebar (User Management is hidden for non-admins).
3. **Register a New Product:**
   - Navigate to **Register Product**.
   - Enter Product Name: `"Logitech Ergonomic Keyboard"`, SKU: `"LOG-ERG-001"`.
   - Category: `"Electronics"`, Initial Qty: `20`, Reorder: `8`, Critical: `3`.
   - Cost: `$45.00`, Selling: `$69.99`.
   - Click **Register Product**. Show success dialog.
4. **View in Inventory:**
   - Navigate to **Products**.
   - Search for `"LOG-ERG-001"`. Show live `NORMAL` green status badge.
5. **Simulate Outbound Dispatch & Alert Trigger:**
   - Navigate to **Stock Movements**.
   - Select `"LOG-ERG-001"`, Movement Type: `DISPATCH`, Quantity: `15`, Ref: `INV-1001`.
   - Submit. New stock is 5 (which is below reorder threshold 8!).
   - Navigate to **Alerts**. Show that the system automatically generated a `LOW_STOCK` warning for `"LOG-ERG-001"`.
6. **Attempt Invalid Negative Stock Dispatch:**
   - Attempt to dispatch `10` units when only `5` are available.
   - Show error dialog: `"Insufficient stock. Available: 5, requested: 10"`.
   - Show that stock remains at 5 (transaction rollback).
7. **Demand Analytics & Reorders:**
   - Navigate to **Demand Analysis**.
   - Show moving-average daily run rates, velocity badges, and suggested reorder quantities.
8. **Admin Features & Disaster Recovery:**
   - Log out, sign in as `admin` / `Admin@123`.
   - Show **User Management** (add user, change password).
   - Navigate to **Settings & Maintenance** -> click **Create Backup Now**.
   - Show instant SQLite database backup created.
9. **Export CSV:**
   - Navigate to **Reports** -> select **Inventory Valuation Report** -> click **Export to CSV**.
   - Open generated CSV in Excel or Notepad to demonstrate UTF-8 BOM formatting.

---

## 3. Viva Q&A Preparation Cheat Sheet

#### Q1: Why did you choose Java Swing instead of a web framework?
**Answer:** Swing is standard in Java SE, requiring no external web servers, browser runtimes, or Node.js dependencies. It runs directly as a fast, responsive, native desktop executable suitable for offline warehouse terminals and POS cash registers.

#### Q2: What OOP principles are implemented in this project?
**Answer:**
- **Encapsulation:** All entities (`Product`, `StockMovement`, etc.) have private attributes with validated getters and setters.
- **Abstraction:** The database service layer hides JDBC boilerplate and connection pooling behind clean service interfaces.
- **Inheritance:** Swing components inherit from `JPanel`, `JFrame`, and `DefaultTableModel`.
- **Polymorphism:** The `Page` interface (`id()`, `title()`, `component()`, `refresh()`) is implemented by 11 pages and managed polymorphically via a `CardLayout` in `MainFrame`.

#### Q3: Why SQLite over MySQL or PostgreSQL?
**Answer:** SQLite is an embedded, serverless, self-contained relational database. It requires zero configuration, stores data in a single cross-platform file, supports full ACID transactions, and eliminates external database server setup for the client.

#### Q4: How do you prevent SQL Injection?
**Answer:** Every single query in the application uses parameterized `PreparedStatement` with `?` placeholders. User input is never concatenated directly into SQL query strings.

#### Q5: How do you handle database transactions?
**Answer:** In `Database.inTransaction(connection -> ...)`, we disable auto-commit (`connection.setAutoCommit(false)`), execute the required SQL statements, and call `connection.commit()`. If any runtime or SQL exception occurs, `connection.rollback()` is executed in the catch block, ensuring data integrity.

#### Q6: Why store prices as integer cents instead of float or double?
**Answer:** Floating-point representations (IEEE 754) suffer from binary rounding imprecision (e.g. `0.1 + 0.2 != 0.3`). Storing prices as exact integer cents eliminates rounding errors completely.
