package com.intellistock.util;

import java.util.regex.Pattern;

public final class Validation {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{7,20}$");

    private Validation() {
    }

    public static String required(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    public static int nonNegativeInt(String label, String value) {
        try {
            int parsed = Integer.parseInt(required(label, value));
            if (parsed < 0) {
                throw new IllegalArgumentException(label + " cannot be negative");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a whole number");
        }
    }

    public static int positiveInt(String label, String value) {
        int parsed = nonNegativeInt(label, value);
        if (parsed <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero");
        }
        return parsed;
    }

    public static void optionalEmail(String value) {
        if (value != null && !value.isBlank() && !EMAIL.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Email format looks invalid");
        }
    }

    public static void optionalPhone(String value) {
        if (value != null && !value.isBlank() && !PHONE.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Phone number format looks invalid");
        }
    }

    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
