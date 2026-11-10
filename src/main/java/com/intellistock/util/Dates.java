package com.intellistock.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public final class Dates {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private Dates() {
    }

    public static String now() {
        return LocalDateTime.now().format(DATE_TIME);
    }

    public static String today() {
        return LocalDate.now().format(DATE);
    }

    public static Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.trim(), DATE));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    public static LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value, DATE_TIME);
    }

    public static String formatDisplay(String storedDateTime) {
        if (storedDateTime == null || storedDateTime.isBlank()) {
            return "";
        }
        try {
            return parseDateTime(storedDateTime).format(DISPLAY);
        } catch (Exception ignored) {
            return storedDateTime;
        }
    }
}
