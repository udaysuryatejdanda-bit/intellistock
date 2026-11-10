package com.intellistock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money() {
    }

    public static long toCents(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("Amount is required");
        }
        BigDecimal value = new BigDecimal(amount.trim());
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static String format(long cents) {
        BigDecimal value = BigDecimal.valueOf(cents, 2);
        return NumberFormat.getCurrencyInstance(Locale.US).format(value);
    }

    public static String toPlain(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }
}
