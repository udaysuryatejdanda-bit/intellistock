package com.intellistock.security;

import com.intellistock.model.Role;
import com.intellistock.model.User;

public final class Permissions {
    private Permissions() {
    }

    public static void requireUser(User user) {
        if (user == null || !user.isActive()) {
            throw new SecurityException("You must be signed in");
        }
    }

    public static void requireAdmin(User user) {
        requireUser(user);
        if (user.getRole() != Role.ADMIN) {
            throw new SecurityException("This action is limited to administrators");
        }
    }

    public static boolean canManageUsers(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public static boolean canRegisterProducts(User user) {
        return user != null && user.isActive();
    }

    public static boolean canAdjustStock(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public static boolean canManagePurchaseOrders(User user) {
        return user != null && user.isActive();
    }

    public static boolean canBackup(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }
}
