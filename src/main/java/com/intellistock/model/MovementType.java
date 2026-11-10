package com.intellistock.model;

public enum MovementType {
    INITIAL_STOCK(true),
    RECEIPT(true),
    DISPATCH(false),
    RETURN(true),
    ADJUSTMENT(true),
    DAMAGED(false),
    CORRECTION(true),
    PO_RECEIPT(true);

    private final boolean canIncrease;

    MovementType(boolean canIncrease) {
        this.canIncrease = canIncrease;
    }

    public boolean canIncrease() {
        return canIncrease;
    }

    public boolean isOutbound() {
        return this == DISPATCH || this == DAMAGED;
    }
}
