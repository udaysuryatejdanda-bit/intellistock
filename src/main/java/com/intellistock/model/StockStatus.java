package com.intellistock.model;

public enum StockStatus {
    OUT_OF_STOCK,
    CRITICAL,
    LOW,
    NORMAL,
    ARCHIVED;

    public static StockStatus of(Product product) {
        if (product.isArchived()) {
            return ARCHIVED;
        }
        int qty = product.getQuantity();
        if (qty <= 0) {
            return OUT_OF_STOCK;
        }
        if (qty <= product.getCriticalThreshold()) {
            return CRITICAL;
        }
        if (qty <= product.getReorderThreshold()) {
            return LOW;
        }
        return NORMAL;
    }
}
