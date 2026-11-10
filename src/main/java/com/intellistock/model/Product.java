package com.intellistock.model;

public class Product {
    private long id;
    private String sku;
    private String name;
    private long categoryId;
    private String categoryName;
    private String description;
    private String unit;
    private String barcode;
    private int quantity;
    private int reorderThreshold;
    private int criticalThreshold;
    private String warehouseLocation;
    private String rackShelf;
    private String expiryDate;
    private long costPriceCents;
    private long sellingPriceCents;
    private Long preferredSupplierId;
    private String preferredSupplierName;
    private boolean archived;
    private String createdAt;
    private String updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(int reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public int getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(int criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public String getRackShelf() {
        return rackShelf;
    }

    public void setRackShelf(String rackShelf) {
        this.rackShelf = rackShelf;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public long getCostPriceCents() {
        return costPriceCents;
    }

    public void setCostPriceCents(long costPriceCents) {
        this.costPriceCents = costPriceCents;
    }

    public long getSellingPriceCents() {
        return sellingPriceCents;
    }

    public void setSellingPriceCents(long sellingPriceCents) {
        this.sellingPriceCents = sellingPriceCents;
    }

    public Long getPreferredSupplierId() {
        return preferredSupplierId;
    }

    public void setPreferredSupplierId(Long preferredSupplierId) {
        this.preferredSupplierId = preferredSupplierId;
    }

    public String getPreferredSupplierName() {
        return preferredSupplierName;
    }

    public void setPreferredSupplierName(String preferredSupplierName) {
        this.preferredSupplierName = preferredSupplierName;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public StockStatus getStockStatus() {
        return StockStatus.of(this);
    }

    @Override
    public String toString() {
        return sku + " — " + name;
    }
}
