package com.intellistock.analytics;

import com.intellistock.config.Database;
import com.intellistock.model.Product;
import com.intellistock.repository.ProductRepository;
import com.intellistock.repository.SettingsRepository;
import com.intellistock.repository.StockMovementRepository;
import com.intellistock.util.Dates;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Transparent moving-average demand analysis. This is not machine learning.
 * Baseline = average daily outbound quantity over the previous comparison window.
 * Unusual demand = recent average is at least highDemandRatio times the baseline,
 * and both windows contain outbound activity.
 */
public class DemandAnalytics {
    private final Database database;
    private final ProductRepository products = new ProductRepository();
    private final StockMovementRepository movements = new StockMovementRepository();
    private final SettingsRepository settings = new SettingsRepository();

    public DemandAnalytics(Database database) {
        this.database = database;
    }

    public int windowDays() {
        return database.inTransaction(connection ->
                Integer.parseInt(settings.get(connection, "demand.window.days").orElse("14")));
    }

    public double highDemandRatio() {
        return database.inTransaction(connection ->
                Double.parseDouble(settings.get(connection, "demand.high.ratio").orElse("1.5")));
    }

    public List<ProductDemand> analyze() {
        return database.inTransaction(this::analyze);
    }

    public List<ProductDemand> analyze(Connection connection) throws Exception {
        int window = Integer.parseInt(settings.get(connection, "demand.window.days").orElse("14"));
        double ratio = Double.parseDouble(settings.get(connection, "demand.high.ratio").orElse("1.5"));
        int leadTime = Integer.parseInt(settings.get(connection, "supplier.lead.days").orElse("7"));
        LocalDate today = LocalDate.now();
        String recentFrom = today.minusDays(window).atStartOfDay().format(Dates.DATE_TIME);
        String previousFrom = today.minusDays(window * 2L).atStartOfDay().format(Dates.DATE_TIME);
        String now = today.plusDays(1).atStartOfDay().format(Dates.DATE_TIME);
        List<ProductDemand> rows = new ArrayList<>();
        for (Product product : products.findAll(connection, false)) {
            int recentOutbound = movements.sumOutbound(connection, product.getId(), recentFrom, now);
            int previousOutbound = movements.sumOutbound(connection, product.getId(), previousFrom, recentFrom);
            double recentDaily = recentOutbound / (double) window;
            double baselineDaily = previousOutbound / (double) window;
            boolean enoughHistory = previousOutbound > 0 && recentOutbound > 0;
            boolean highDemand = enoughHistory && recentDaily >= baselineDaily * ratio && recentDaily > 0;
            Double daysRemaining = recentDaily > 0 ? product.getQuantity() / recentDaily : null;
            Integer suggestedReorder = null;
            String forecastNote;
            if (recentDaily <= 0) {
                forecastNote = "No outbound movements in the last " + window
                        + " days. A forecast is not produced until sales or dispatches are recorded.";
            } else {
                int safety = Math.max(product.getCriticalThreshold(), (int) Math.ceil(recentDaily * 3));
                int target = (int) Math.ceil(recentDaily * (leadTime + window)) + safety;
                suggestedReorder = Math.max(0, target - product.getQuantity());
                forecastNote = "Moving average of daily outbound quantity over " + window
                        + " days. Suggested reorder uses average daily demand × (lead time "
                        + leadTime + " + review window) plus safety stock.";
            }
            ProductDemand demand = new ProductDemand();
            demand.product = product;
            demand.recentOutbound = recentOutbound;
            demand.previousOutbound = previousOutbound;
            demand.recentDailyAverage = recentDaily;
            demand.baselineDailyAverage = baselineDaily;
            demand.highDemand = highDemand;
            demand.enoughHistory = enoughHistory;
            demand.daysRemaining = daysRemaining;
            demand.suggestedReorder = suggestedReorder;
            demand.explanation = forecastNote;
            if (recentOutbound == 0) {
                demand.velocity = "NO_MOVEMENT";
            } else if (recentDaily >= 2) {
                demand.velocity = "FAST";
            } else if (recentDaily >= 0.4) {
                demand.velocity = "MEDIUM";
            } else {
                demand.velocity = "SLOW";
            }
            rows.add(demand);
        }
        rows.sort(Comparator.comparingInt((ProductDemand d) -> d.recentOutbound).reversed());
        return rows;
    }

    public static final class ProductDemand {
        public Product product;
        public int recentOutbound;
        public int previousOutbound;
        public double recentDailyAverage;
        public double baselineDailyAverage;
        public boolean highDemand;
        public boolean enoughHistory;
        public Double daysRemaining;
        public Integer suggestedReorder;
        public String velocity;
        public String explanation;
    }
}
