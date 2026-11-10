# IntelliStock AI — Automated Test Results & Test Cases

## 1. Test Suite Summary

- **Test Framework:** JUnit 5 (Jupiter 5.11.4) via Maven Surefire Plugin 3.5.2
- **Java Platform:** OpenJDK 21 LTS (64-bit)
- **Database Engine:** SQLite 3.47.1.0 (In-Memory & Dedicated Temp File Integration Testing)
- **Execution Date:** October 25, 2026
- **Total Tests Executed:** 20
- **Total Tests Passed:** 20
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Overall Result:** **`BUILD SUCCESS (100% PASS)`**

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.intellistock.AlertEngineTest (2 tests) - PASSED
Running com.intellistock.AuthServiceTest (5 tests) - PASSED
Running com.intellistock.DemandAnalyticsTest (1 test) - PASSED
Running com.intellistock.EndToEndLiveTest (1 test) - PASSED
Running com.intellistock.InventoryServiceTest (5 tests) - PASSED
Running com.intellistock.ProductServiceTest (4 tests) - PASSED
Running com.intellistock.PurchaseOrderServiceTest (2 tests) - PASSED

Results:
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 2. Test Cases Specification & Verification Matrix

| Test ID | Class / Module | Test Case Name | Input / Precondition | Expected Behavior | Actual Behavior | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-01** | `ProductServiceTest` | `testRegisterProductWithInitialStock` | Valid request with SKU `STAND-001`, initial quantity = 25, reorder threshold = 10. | Product registered in SQLite; quantity = 25; 1 `INITIAL_STOCK` movement created atomically. | Product inserted with ID; stock movement created with delta=+25; previous=0; resulting=25. | **PASS** |
| **TC-02** | `ProductServiceTest` | `testDuplicateSkuRejection` | Attempt to register two distinct products with identical SKU `DUP-SKU-999`. | Second registration throws `IllegalArgumentException("SKU already exists")`; transaction rolls back. | Exception caught; second record not created; database state consistent. | **PASS** |
| **TC-03** | `ProductServiceTest` | `testInvalidThresholdsValidation` | Critical threshold (8) configured greater than reorder threshold (5). | Throws `IllegalArgumentException("Critical threshold cannot exceed...")`. | Exception thrown and verified before database insert. | **PASS** |
| **TC-04** | `ProductServiceTest` | `testUpdateMetadataAndArchiving` | Update name and warehouse location; then call `archive()`. | Metadata updated with quantity preserved; archive flag set to true (`StockStatus.ARCHIVED`). | Name and location updated; quantity intact at 10; status transitions to ARCHIVED. | **PASS** |
| **TC-05** | `InventoryServiceTest` | `testStockReceipt` | Inbound receipt of 25 units for product with quantity 50. | Stock increases to 75; `RECEIPT` movement recorded; delta = +25. | Resulting quantity = 75; delta = +25; audit log inserted. | **PASS** |
| **TC-06** | `InventoryServiceTest` | `testStockDispatch` | Customer dispatch of 15 units for product with quantity 50. | Stock decreases to 35; `DISPATCH` movement recorded; delta = -15. | Resulting quantity = 35; delta = -15; movement logged. | **PASS** |
| **TC-07** | `InventoryServiceTest` | `testNegativeStockPrevention` | Current stock = 50. Attempt dispatch of 51 units. | Throws `IllegalArgumentException("Insufficient stock")`; stock remains 50. | Exception thrown; transaction rolled back; quantity verified at 50. | **PASS** |
| **TC-08** | `InventoryServiceTest` | `testDamagedStockRequiresReason` | 1) Damaged movement with empty reason. 2) Damaged with valid explanation. | 1) Throws `IllegalArgumentException`. 2) Deducts stock to 48. | Empty reason rejected; valid reason records `DAMAGED` movement with delta = -2. | **PASS** |
| **TC-09** | `InventoryServiceTest` | `testAdjustmentSecurityAndExecution` | 1) Staff user attempts adjustment. 2) Admin user adjusts stock by -10. | 1) Throws `SecurityException`. 2) Stock adjusted to 40 with audit log. | Staff permission rejected; admin adjustment successful. | **PASS** |
| **TC-10** | `AlertEngineTest` | `testLowAndCriticalStockAlertGeneration` | Product initially 15 units (reorder=10, crit=3). Dispatch 7 -> qty 8. Dispatch 6 -> qty 2. Receive 20 -> qty 22. | Qty 8 triggers `LOW_STOCK` (Warning). Qty 2 triggers `CRITICAL_STOCK` (Critical). Qty 22 resolves both alerts. | Alerts accurately generated, updated, and automatically resolved upon stock increase. | **PASS** |
| **TC-11** | `AlertEngineTest` | `testAcknowledgeAndResolveAlert` | Active out-of-stock alert acknowledged then resolved. | Status transitions: `ACTIVE` -> `ACKNOWLEDGED` -> `RESOLVED`. | Alert states verified after each lifecycle method call. | **PASS** |
| **TC-12** | `PurchaseOrderServiceTest` | `testPurchaseOrderFullLifecycle` | PO for 100 rolls of tape. Submit. Partial receipt of 40. Final receipt of 60. | Draft -> Pending -> Partially Received (qty+40) -> Received (qty+60). Total stock = 110. | State transitions verified; stock movements recorded with `PO_RECEIPT`. | **PASS** |
| **TC-13** | `PurchaseOrderServiceTest` | `testCannotReceiveMoreThanOrdered` | PO for 30 units. Attempt to receive 31 units. | Throws `IllegalArgumentException("Cannot receive more than outstanding...")`. | Receipt blocked; database unchanged. | **PASS** |
| **TC-14** | `DemandAnalyticsTest` | `testDemandVelocityAndReorderProjection` | Fast item: 30 dispatches in 14 days. Slow item: 2 dispatches in 14 days. | Fast daily average > 2.0 (`FAST`). Slow daily average = 0.14 (`SLOW`). | Velocity classifications and run-rate projections computed accurately. | **PASS** |
| **TC-15** | `AuthServiceTest` | `testSuccessfulLoginAndLogout` | Valid credentials for admin user. Call `logout()`. | Returns authenticated user without password hash. `currentUser()` null after logout. | Successful login; hash sanitized; logout clears session context. | **PASS** |
| **TC-16** | `AuthServiceTest` | `testInvalidPasswordFails` | Incorrect password supplied for valid username. | Throws `IllegalArgumentException("Invalid username or password")`. | Authentication rejected; security audit logged. | **PASS** |
| **TC-17** | `AuthServiceTest` | `testUnknownUserFails` | Non-existent username supplied. | Throws `IllegalArgumentException("Invalid username or password")`. | Authentication rejected uniformly (prevents user enumeration). | **PASS** |
| **TC-18** | `AuthServiceTest` | `testInactiveUserCannotLogin` | Account deactivated by administrator attempts login. | Throws `IllegalArgumentException("Invalid username or password")`. | Deactivated user blocked from signing in. | **PASS** |
| **TC-19** | `AuthServiceTest` | `testRolePermissions` | Test `ADMIN` vs `STAFF` permissions across modules. | `ADMIN` can manage users & adjust stock. `STAFF` restricted. | Role checks verified against `Permissions` utility. | **PASS** |
| **TC-20** | `EndToEndLiveTest` | `testFullApplicationWorkflowWithDemoDataAndPersistence` | End-to-end integration test: seed demo data, login, register new product, receive, dispatch, verify alerts, export CSV. | Full lifecycle runs flawlessly against SQLite database with JDBC persistence. | All steps verified; database state confirmed. | **PASS** |

---

## 3. Conclusions

All automated test suites demonstrate 100% compliance with business rules, relational foreign key constraints, ACID transaction atomicity, and role-based access control.
