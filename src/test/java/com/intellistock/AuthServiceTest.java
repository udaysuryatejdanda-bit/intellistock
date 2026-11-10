package com.intellistock;

import com.intellistock.config.AppContext;
import com.intellistock.config.Database;
import com.intellistock.model.Role;
import com.intellistock.model.User;
import com.intellistock.security.Permissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    @TempDir
    Path tempDir;

    private AppContext context;
    private User admin;
    private User staff;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test-auth.db");
        Database db = new Database(dbPath);
        context = AppContext.forTesting(db);

        User bootstrap = new User();
        bootstrap.setId(1);
        bootstrap.setRole(Role.ADMIN);
        bootstrap.setActive(true);

        admin = context.users().create(bootstrap, "adminuser", "Admin", Role.ADMIN, "SecureAdmin@123".toCharArray());
        staff = context.users().create(bootstrap, "staffuser", "Staff", Role.STAFF, "SecureStaff@123".toCharArray());
    }

    @Test
    void testSuccessfulLoginAndLogout() {
        User loggedIn = context.auth().login("adminuser", "SecureAdmin@123".toCharArray());
        assertNotNull(loggedIn);
        assertEquals("adminuser", loggedIn.getUsername());
        assertEquals(Role.ADMIN, loggedIn.getRole());
        assertNull(loggedIn.getPasswordHash()); // Sensitive hash not exposed to UI

        assertTrue(context.auth().currentUser() != null);
        assertEquals("adminuser", context.auth().currentUser().getUsername());

        context.auth().logout();
        assertNull(context.auth().currentUser());
        assertThrows(SecurityException.class, () -> context.auth().requireSignedIn());
    }

    @Test
    void testInvalidPasswordFails() {
        assertThrows(IllegalArgumentException.class, () ->
                context.auth().login("adminuser", "WrongPassword@123".toCharArray()));
    }

    @Test
    void testUnknownUserFails() {
        assertThrows(IllegalArgumentException.class, () ->
                context.auth().login("nonexistent", "SomePassword@123".toCharArray()));
    }

    @Test
    void testInactiveUserCannotLogin() {
        context.users().setActive(admin, staff.getId(), false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                context.auth().login("staffuser", "SecureStaff@123".toCharArray()));
        assertTrue(ex.getMessage().contains("Invalid username or password"));
    }

    @Test
    void testRolePermissions() {
        assertTrue(Permissions.canRegisterProducts(admin));
        assertTrue(Permissions.canRegisterProducts(staff));

        assertTrue(Permissions.canManageUsers(admin));
        assertFalse(Permissions.canManageUsers(staff));
    }
}
