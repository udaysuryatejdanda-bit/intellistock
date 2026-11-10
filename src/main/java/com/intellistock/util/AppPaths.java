package com.intellistock.util;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {
    private static Path resolvedRoot;

    private AppPaths() {
    }

    public static synchronized Path dataRoot() {
        if (resolvedRoot != null) {
            return resolvedRoot;
        }
        String override = System.getProperty("intellistock.data.dir");
        if (override != null && !override.isBlank()) {
            resolvedRoot = Path.of(override).toAbsolutePath();
            return resolvedRoot;
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            try {
                Path target = Path.of(localAppData, "IntelliStockAI");
                Files.createDirectories(target);
                resolvedRoot = target;
                return resolvedRoot;
            } catch (Exception ignored) {
                // fall back to project directory if LOCALAPPDATA is not writable
            }
        }
        resolvedRoot = Path.of(System.getProperty("user.dir", "."), "app_data").toAbsolutePath();
        return resolvedRoot;
    }

    public static Path databaseFile() {
        return dataRoot().resolve("data").resolve("intellistock.db");
    }

    public static Path backupDir() {
        return dataRoot().resolve("backups");
    }

    public static Path logDir() {
        return dataRoot().resolve("logs");
    }

    public static Path reportsDir() {
        return dataRoot().resolve("reports");
    }

    public static void ensureDirectories() {
        try {
            Files.createDirectories(databaseFile().getParent());
            Files.createDirectories(backupDir());
            Files.createDirectories(logDir());
            Files.createDirectories(reportsDir());
        } catch (Exception error) {
            throw new IllegalStateException("Unable to create application data directories", error);
        }
    }
}
