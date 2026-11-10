package com.intellistock.model;

public class ProductRegistrationRequest {
    private String name;
    private String sku;
    private Long categoryId;
    private String newCategoryName;
    private String description;
    private String unit;
    private String barcode;
    private int initialQuantity;
    private int reorderThreshold;
    private Integer criticalThreshold;
    private String warehouseLocation;
    private String rackShelf;
    private String expiryDate;
    private long costPriceCents;
    private long sellingPriceCents;
    private Long preferredSupplierId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getNewCategoryName() {
        return newCategoryName;
    }

    public void setNewCategoryName(String newCategoryName) {
        this.newCategoryName = newCategoryName;
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

    public int getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(int initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public int getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(int reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public Integer getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(Integer criticalThreshold) {
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
}
