package com.intellistock.config;

import com.intellistock.util.AppLogger;
import com.intellistock.util.AppPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Level;

public final class Database {
    private final String jdbcUrl;
    private final Path databasePath;

    public Database(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath);
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }

    public static Database applicationDatabase() {
        AppPaths.ensureDirectories();
        return new Database(AppPaths.databaseFile());
    }

    public Path path() {
        return databasePath;
    }

    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public void initializeSchema() {
        try {
            Files.createDirectories(databasePath.getParent());
        } catch (Exception error) {
            throw new IllegalStateException("Cannot create database directory", error);
        }
        try (Connection connection = open();
             Statement statement = connection.createStatement();
             InputStream stream = Database.class.getResourceAsStream("/database/schema.sql")) {
            if (stream == null) {
                throw new IllegalStateException("schema.sql is missing from the application resources");
            }
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA foreign_keys = ON");
            for (String batch : sql.split(";")) {
                String trimmed = batch.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    statement.execute(trimmed);
                }
            }
            try (var rs = statement.executeQuery("SELECT COUNT(*) FROM schema_info")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    statement.executeUpdate("INSERT INTO schema_info(version) VALUES (1)");
                }
            }
        } catch (Exception error) {
            AppLogger.get().log(Level.SEVERE, "Database initialization failed", error);
            throw new IllegalStateException("Unable to initialize the database", error);
        }
    }

    public <T> T inTransaction(SqlWork<T> work) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Exception error) {
                connection.rollback();
                if (error instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException(error);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Database transaction failed", error);
        }
    }

    public void execute(SqlAction action) {
        inTransaction(connection -> {
            action.run(connection);
            return null;
        });
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws Exception;
    }

    @FunctionalInterface
    public interface SqlAction {
        void run(Connection connection) throws Exception;
    }
}
