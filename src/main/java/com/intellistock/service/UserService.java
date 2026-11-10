package com.intellistock.service;

import com.intellistock.config.Database;
import com.intellistock.model.Role;
import com.intellistock.model.User;
import com.intellistock.repository.AuditRepository;
import com.intellistock.repository.UserRepository;
import com.intellistock.security.PasswordHasher;
import com.intellistock.security.Permissions;
import com.intellistock.util.Dates;
import com.intellistock.util.Validation;

import java.util.Arrays;
import java.util.List;

public class UserService {
    private final Database database;
    private final UserRepository users = new UserRepository();
    private final AuditRepository audit = new AuditRepository();
    private final PasswordHasher hasher = new PasswordHasher();

    public UserService(Database database) {
        this.database = database;
    }

    public List<User> list(User actor) {
        Permissions.requireAdmin(actor);
        return database.inTransaction(connection -> {
            List<User> all = users.findAll(connection);
            all.forEach(user -> user.setPasswordHash(null));
            return all;
        });
    }

    public User create(User actor, String username, String fullName, Role role, char[] password) {
        Permissions.requireAdmin(actor);
        try {
            Validation.required("Username", username);
            Validation.required("Full name", fullName);
            if (password == null || password.length < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }
            return database.inTransaction(connection -> {
                if (users.findByUsername(connection, username).isPresent()) {
                    throw new IllegalArgumentException("Username already exists");
                }
                User user = new User();
                user.setUsername(username.trim());
                user.setFullName(fullName.trim());
                user.setRole(role);
                user.setActive(true);
                user.setCreatedAt(Dates.now());
                user.setPasswordHash(hasher.hash(password));
                long id = users.insert(connection, user);
                user.setId(id);
                user.setPasswordHash(null);
                audit.insert(connection, actor.getId(), "USER_CREATE", "USER", id, username);
                return user;
            });
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    public void changePassword(User actor, long userId, char[] password) {
        Permissions.requireAdmin(actor);
        try {
            if (password == null || password.length < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }
            database.execute(connection -> {
                User user = users.findById(connection, userId)
                        .orElseThrow(() -> new IllegalArgumentException("User was not found"));
                user.setPasswordHash(hasher.hash(password));
                users.update(connection, user);
                audit.insert(connection, actor.getId(), "USER_PASSWORD", "USER", userId, "Password changed");
            });
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    public void setActive(User actor, long userId, boolean active) {
        Permissions.requireAdmin(actor);
        if (actor.getId() == userId && !active) {
            throw new IllegalArgumentException("You cannot deactivate your own account");
        }
        database.execute(connection -> {
            User user = users.findById(connection, userId)
                    .orElseThrow(() -> new IllegalArgumentException("User was not found"));
            user.setActive(active);
            users.update(connection, user);
            audit.insert(connection, actor.getId(), active ? "USER_ACTIVATE" : "USER_DEACTIVATE", "USER", userId, null);
        });
    }
}
