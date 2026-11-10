package com.intellistock.service;

import com.intellistock.config.Database;
import com.intellistock.model.User;
import com.intellistock.repository.AuditRepository;
import com.intellistock.security.Permissions;
import com.intellistock.util.AppPaths;
import com.intellistock.util.Dates;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {
    private final Database database;
    private final AuditRepository audit = new AuditRepository();

    public BackupService(Database database) {
        this.database = database;
    }

    public Path backupDestination() {
        AppPaths.ensureDirectories();
        return AppPaths.backupDir();
    }

    public Path createBackup(User user) {
        Permissions.requireAdmin(user);
        AppPaths.ensureDirectories();
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path target = AppPaths.backupDir().resolve("intellistock-" + stamp + ".db");
        vacuumInto(database.path(), target);
        database.execute(connection ->
                audit.insert(connection, user.getId(), "BACKUP", "DATABASE", null, target.toString()));
        return target;
    }

    public void restore(User user, Path backupFile) {
        Permissions.requireAdmin(user);
        validateBackup(backupFile);
        Path safety = createBackup(user);
        try {
            Path live = database.path();
            Path temp = live.resolveSibling("intellistock.restore.tmp");
            Files.copy(backupFile, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, live, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception error) {
            throw new IllegalStateException(
                    "Restore failed. A safety copy of the previous database is at " + safety, error);
        }
        database.initializeSchema();
        database.execute(connection ->
                audit.insert(connection, user.getId(), "RESTORE", "DATABASE", null, backupFile.toString()));
    }

    public void validateBackup(Path backupFile) {
        if (backupFile == null || !Files.isRegularFile(backupFile)) {
            throw new IllegalArgumentException("Select a valid backup file");
        }
        String url = "jdbc:sqlite:" + backupFile.toAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='products'")) {
            if (!rs.next()) {
                throw new IllegalArgumentException("The selected file is not a valid IntelliStock backup");
            }
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("The selected file could not be opened as a SQLite database");
        }
    }

    private void vacuumInto(Path source, Path target) {
        String url = "jdbc:sqlite:" + source.toAbsolutePath();
        String destination = target.toAbsolutePath().toString().replace("'", "''");
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(FULL)");
            statement.executeUpdate("VACUUM INTO '" + destination + "'");
        } catch (Exception error) {
            throw new IllegalStateException("SQLite backup failed", error);
        }
    }

    public String lastBackupHint() {
        return "Backups are stored in " + AppPaths.backupDir() + ". Created at " + Dates.now() + ".";
    }
}
