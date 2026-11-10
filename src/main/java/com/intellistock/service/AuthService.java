package com.intellistock.service;

import com.intellistock.config.Database;
import com.intellistock.model.User;
import com.intellistock.repository.AuditRepository;
import com.intellistock.repository.UserRepository;
import com.intellistock.security.PasswordHasher;
import com.intellistock.security.Permissions;

import java.util.Arrays;

public class AuthService {
    private final Database database;
    private final UserRepository users = new UserRepository();
    private final AuditRepository audit = new AuditRepository();
    private final PasswordHasher hasher = new PasswordHasher();
    private User currentUser;

    public AuthService(Database database) {
        this.database = database;
    }

    public User login(String username, char[] password) {
        try {
            return database.inTransaction(connection -> {
                User user = users.findByUsername(connection, username)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
                if (!user.isActive() || !hasher.verify(password, user.getPasswordHash())) {
                    throw new IllegalArgumentException("Invalid username or password");
                }
                audit.insert(connection, user.getId(), "LOGIN", "USER", user.getId(), "Successful login");
                currentUser = user;
                currentUser.setPasswordHash(null);
                return currentUser;
            });
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    public void logout() {
        User user = currentUser;
        currentUser = null;
        if (user != null) {
            database.execute(connection ->
                    audit.insert(connection, user.getId(), "LOGOUT", "USER", user.getId(), "User signed out"));
        }
    }

    public User currentUser() {
        return currentUser;
    }

    public void requireSignedIn() {
        Permissions.requireUser(currentUser);
    }
}
