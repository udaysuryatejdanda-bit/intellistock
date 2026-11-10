package com.intellistock.util;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {
    private static final Logger LOGGER = Logger.getLogger("com.intellistock");
    private static boolean configured;

    private AppLogger() {
    }

    public static synchronized Logger get() {
        if (!configured) {
            LOGGER.setUseParentHandlers(true);
            LOGGER.setLevel(Level.INFO);
            try {
                Files.createDirectories(AppPaths.logDir());
                FileHandler handler = new FileHandler(
                        AppPaths.logDir().resolve("intellistock.log").toString(), 1_000_000, 3, true);
                handler.setFormatter(new SimpleFormatter());
                LOGGER.addHandler(handler);
            } catch (IOException ignored) {
                // Console logging remains available.
            }
            configured = true;
        }
        return LOGGER;
    }
}
