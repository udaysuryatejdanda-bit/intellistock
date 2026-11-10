package com.intellistock.service;

import com.intellistock.analytics.DemandAnalytics;
import com.intellistock.config.Database;
import com.intellistock.model.Product;
import com.intellistock.model.PurchaseOrder;
import com.intellistock.model.StockMovement;
import com.intellistock.model.StockStatus;
import com.intellistock.model.Supplier;
import com.intellistock.repository.ProductRepository;
import com.intellistock.repository.PurchaseOrderRepository;
import com.intellistock.repository.StockMovementRepository;
import com.intellistock.repository.SupplierRepository;
import com.intellistock.util.Money;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {
    private final Database database;
    private final ProductRepository products = new ProductRepository();
    private final StockMovementRepository movements = new StockMovementRepository();
    private final SupplierRepository suppliers = new SupplierRepository();
    private final PurchaseOrderRepository purchaseOrders = new PurchaseOrderRepository();
    private final DemandAnalytics demandAnalytics;

    public ReportService(Database database, DemandAnalytics demandAnalytics) {
        this.database = database;
        this.demandAnalytics = demandAnalytics;
    }

    public List<String[]> inventoryReport() {
        return database.inTransaction(connection -> {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"SKU", "Name", "Category", "Quantity", "Unit", "Cost", "Selling", "Status", "Location"});
            for (Product product : products.findAll(connection, false)) {
                rows.add(new String[]{
                        product.getSku(), product.getName(), product.getCategoryName(),
                        String.valueOf(product.getQuantity()), product.getUnit(),
                        Money.format(product.getCostPriceCents()), Money.format(product.getSellingPriceCents()),
                        product.getStockStatus().name(), n(product.getWarehouseLocation())
                });
            }
            return rows;
        });
    }

    public List<String[]> stockStatusReport(StockStatus status) {
        return database.inTransaction(connection -> {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"SKU", "Name", "Quantity", "Reorder", "Critical", "Status"});
            for (Product product : products.findAll(connection, false)) {
                if (product.getStockStatus() == status) {
                    rows.add(new String[]{
                            product.getSku(), product.getName(), String.valueOf(product.getQuantity()),
                            String.valueOf(product.getReorderThreshold()),
                            String.valueOf(product.getCriticalThreshold()), status.name()
                    });
                }
            }
            return rows;
        });
    }

    public List<String[]> valuationReport() {
        return database.inTransaction(connection -> {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"SKU", "Name", "Quantity", "Unit cost", "Extended value"});
            long total = 0;
            for (Product product : products.findAll(connection, false)) {
                long value = product.getQuantity() * product.getCostPriceCents();
                total += value;
                rows.add(new String[]{
                        product.getSku(), product.getName(), String.valueOf(product.getQuantity()),
                        Money.format(product.getCostPriceCents()), Money.format(value)
                });
            }
            rows.add(new String[]{"TOTAL", "", "", "", Money.format(total)});
            return rows;
        });
    }

    public List<String[]> movementReport() {
        return database.inTransaction(connection -> {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"ID", "When", "SKU", "Product", "Type", "Qty", "Previous", "Resulting", "User", "Reason"});
            for (StockMovement movement : movements.findAll(connection)) {
                rows.add(new String[]{
                        String.valueOf(movement.getId()), movement.getOccurredAt(), movement.getSku(),
                        movement.getProductName(), movement.getMovementType().name(),
                        String.valueOf(movement.getQuantityDelta()), String.valueOf(movement.getPreviousQuantity()),
                        String.valueOf(movement.getResultingQuantity()), movement.getUsername(),
                        n(movement.getReason())
                });
            }
            return rows;
        });
    }

    public List<String[]> supplierReport() {
        return database.inTransaction(connection -> {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"ID", "Company", "Contact", "Email", "Phone", "Active"});
            for (Supplier supplier : suppliers.findAll(connection, false)) {
                rows.add(new String[]{
                        String.valueOf(supplier.getId()), supplier.getCompanyName(), n(supplier.getContactPerson()),
                        n(supplier.getEmail()), n(supplier.getPhone()), supplier.isActive() ? "Yes" : "No"
                });
            }
            return rows;
        });
    }

    public List<String[]> purchaseOrderReport() {
        return database.inTransaction(connection -> {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"ID", "Supplier", "Status", "Expected", "Total", "Created"});
            for (PurchaseOrder order : purchaseOrders.findAll(connection)) {
                rows.add(new String[]{
                        String.valueOf(order.getId()), order.getSupplierName(), order.getStatus().name(),
                        n(order.getExpectedDeliveryDate()), Money.format(order.totalCents()), order.getCreatedAt()
                });
            }
            return rows;
        });
    }

    public List<String[]> highDemandReport() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"SKU", "Name", "Recent outbound", "Previous outbound", "High demand", "Suggested reorder"});
        for (DemandAnalytics.ProductDemand demand : demandAnalytics.analyze()) {
            if (demand.highDemand) {
                rows.add(new String[]{
                        demand.product.getSku(), demand.product.getName(),
                        String.valueOf(demand.recentOutbound), String.valueOf(demand.previousOutbound),
                        "Yes", demand.suggestedReorder == null ? "" : String.valueOf(demand.suggestedReorder)
                });
            }
        }
        return rows;
    }

    public List<String[]> expiryReport() {
        return database.inTransaction(connection -> {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"SKU", "Name", "Expiry", "Quantity", "State"});
            LocalDate today = LocalDate.now();
            for (Product product : products.findAll(connection, false)) {
                var date = com.intellistock.util.Dates.parseDate(product.getExpiryDate());
                if (date.isEmpty()) {
                    continue;
                }
                String state = date.get().isBefore(today) ? "EXPIRED"
                        : date.get().isAfter(today.plusDays(30)) ? "OK" : "EXPIRING";
                if (!"OK".equals(state)) {
                    rows.add(new String[]{
                            product.getSku(), product.getName(), product.getExpiryDate(),
                            String.valueOf(product.getQuantity()), state
                    });
                }
            }
            return rows;
        });
    }

    public Path exportCsv(String reportName, List<String[]> rows, Path destination) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        for (String[] row : rows) {
            builder.append(row.length == 0 ? "" : java.util.Arrays.stream(row)
                    .map(this::csv).collect(Collectors.joining(",")));
            builder.append("\r\n");
        }
        Files.writeString(destination, builder.toString(), StandardCharsets.UTF_8);
        return destination;
    }

    private String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private static String n(String value) {
        return value == null ? "" : value;
    }
}
