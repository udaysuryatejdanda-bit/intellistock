package com.intellistock;

import com.intellistock.config.AppContext;
import com.intellistock.model.Role;
import com.intellistock.model.User;

public final class TestHelper {
    private TestHelper() {
    }

    public static User createAdmin(AppContext context, String username) {
        User bootstrap = new User();
        bootstrap.setId(1);
        bootstrap.setRole(Role.ADMIN);
        bootstrap.setActive(true);
        return context.users().create(bootstrap, username, "Admin " + username, Role.ADMIN, "AdminPass@123".toCharArray());
    }

    public static User createStaff(AppContext context, String username) {
        User bootstrap = new User();
        bootstrap.setId(1);
        bootstrap.setRole(Role.ADMIN);
        bootstrap.setActive(true);
        return context.users().create(bootstrap, username, "Staff " + username, Role.STAFF, "StaffPass@123".toCharArray());
    }
}
