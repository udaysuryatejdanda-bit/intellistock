package com.intellistock.model;

public class PurchaseOrderItem {
    private long id;
    private long purchaseOrderId;
    private long productId;
    private String productName;
    private String sku;
    private int quantityOrdered;
    private int quantityReceived;
    private long unitCostCents;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantityOrdered() {
        return quantityOrdered;
    }

    public void setQuantityOrdered(int quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public void setQuantityReceived(int quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public int outstanding() {
        return quantityOrdered - quantityReceived;
    }

    public long getUnitCostCents() {
        return unitCostCents;
    }

    public void setUnitCostCents(long unitCostCents) {
        this.unitCostCents = unitCostCents;
    }

    public long lineTotalCents() {
        return unitCostCents * quantityOrdered;
    }
}
