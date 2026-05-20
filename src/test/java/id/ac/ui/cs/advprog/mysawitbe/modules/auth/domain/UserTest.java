package id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Domain Entity Tests")
class UserTest {

    private User user;
    private UUID userId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        now = LocalDateTime.now();
        user = new User(
                userId,
                "testuser",
                "test@example.com",
                "Test User",
                "hashed_password_123",
                UserRole.BURUH,
                null,
                null,
                now,
                now
        );
    }

    @Test
    @DisplayName("Should create User with all fields")
    void testUserCreationWithAllFields() {
        assertNotNull(user);
        assertEquals(userId, user.getUserId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getName());
        assertEquals("hashed_password_123", user.getHashedPassword());
        assertEquals(UserRole.BURUH, user.getRole());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should create User with default constructor")
    void testUserCreationWithDefaultConstructor() {
        User emptyUser = new User();
        assertNull(emptyUser.getUserId());
        assertNull(emptyUser.getUsername());
    }

    @Test
    @DisplayName("isOAuthAccount() should return false when password is present")
    void testIsOAuthAccountReturnsFalseWhenPasswordPresent() {
        assertFalse(user.isOAuthAccount());
    }

    @Test
    @DisplayName("isOAuthAccount() should return true when password is null")
    void testIsOAuthAccountReturnsTrueWhenPasswordNull() {
        User oauthUser = new User(
                userId,
                "oauthuser",
                "oauth@example.com",
                "OAuth User",
                null,  // null password indicates OAuth account
                UserRole.BURUH,
                null,
                null,
                now,
                now
        );
        assertTrue(oauthUser.isOAuthAccount());
    }

    @Test
    @DisplayName("isAdmin() should return true for ADMIN role")
    void testIsAdminReturnsTrueForAdminRole() {
        User adminUser = new User(
                userId,
                "admin",
                "admin@example.com",
                "Admin User",
                "hashed_password",
                UserRole.ADMIN,
                null,
                null,
                now,
                now
        );
        assertTrue(adminUser.isAdmin());
    }

    @Test
    @DisplayName("isAdmin() should return false for non-ADMIN roles")
    void testIsAdminReturnsFalseForNonAdminRoles() {
        assertFalse(user.isAdmin());

        User mandorUser = new User(
                userId,
                "mandor",
                "mandor@example.com",
                "Mandor User",
                "hashed_password",
                UserRole.MANDOR,
                null,
                null,
                now,
                now
        );
        assertFalse(mandorUser.isAdmin());

        User supirUser = new User(
                userId,
                "supir",
                "supir@example.com",
                "Supir User",
                "hashed_password",
                UserRole.SUPIR,
                null,
                null,
                now,
                now
        );
        assertFalse(supirUser.isAdmin());
    }

    @Test
    @DisplayName("Should set and get userId")
    void testSetAndGetUserId() {
        UUID newUserId = UUID.randomUUID();
        user.setUserId(newUserId);
        assertEquals(newUserId, user.getUserId());
    }

    @Test
    @DisplayName("Should set and get username")
    void testSetAndGetUsername() {
        user.setUsername("newusername");
        assertEquals("newusername", user.getUsername());
    }

    @Test
    @DisplayName("Should set and get email")
    void testSetAndGetEmail() {
        user.setEmail("newemail@example.com");
        assertEquals("newemail@example.com", user.getEmail());
    }

    @Test
    @DisplayName("Should set and get name")
    void testSetAndGetName() {
        user.setName("New Name");
        assertEquals("New Name", user.getName());
    }

    @Test
    @DisplayName("Should set and get hashedPassword")
    void testSetAndGetHashedPassword() {
        user.setHashedPassword("new_hashed_password");
        assertEquals("new_hashed_password", user.getHashedPassword());
    }

    @Test
    @DisplayName("Should set and get role")
    void testSetAndGetRole() {
        user.setRole(UserRole.MANDOR);
        assertEquals(UserRole.MANDOR, user.getRole());
    }

    @Test
    @DisplayName("Should set and get mandorId")
    void testSetAndGetMandorId() {
        UUID mandorId = UUID.randomUUID();
        user.setMandorId(mandorId);
        assertEquals(mandorId, user.getMandorId());
    }

    @Test
    @DisplayName("Should set and get mandorCertificationNumber")
    void testSetAndGetMandorCertificationNumber() {
        String certNumber = "CERT-12345";
        user.setMandorCertificationNumber(certNumber);
        assertEquals(certNumber, user.getMandorCertificationNumber());
    }

    @Test
    @DisplayName("Should set and get createdAt")
    void testSetAndGetCreatedAt() {
        LocalDateTime newTime = LocalDateTime.now().plusDays(1);
        user.setCreatedAt(newTime);
        assertEquals(newTime, user.getCreatedAt());
    }

    @Test
    @DisplayName("Should set and get updatedAt")
    void testSetAndGetUpdatedAt() {
        LocalDateTime newTime = LocalDateTime.now().plusHours(1);
        user.setUpdatedAt(newTime);
        assertEquals(newTime, user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle BURUH user with mandorId")
    void testBuruhUserWithMandorId() {
        UUID mandorId = UUID.randomUUID();
        User buruhUser = new User(
                userId,
                "buruh",
                "buruh@example.com",
                "Buruh User",
                "hashed_password",
                UserRole.BURUH,
                mandorId,
                null,
                now,
                now
        );
        assertEquals(UserRole.BURUH, buruhUser.getRole());
        assertEquals(mandorId, buruhUser.getMandorId());
    }

    @Test
    @DisplayName("Should handle MANDOR user with certification number")
    void testMandorUserWithCertificationNumber() {
        String certNumber = "MANDOR-CERT-2024";
        User mandorUser = new User(
                userId,
                "mandor",
                "mandor@example.com",
                "Mandor User",
                "hashed_password",
                UserRole.MANDOR,
                null,
                certNumber,
                now,
                now
        );
        assertEquals(UserRole.MANDOR, mandorUser.getRole());
        assertEquals(certNumber, mandorUser.getMandorCertificationNumber());
    }

    @Test
    @DisplayName("Should handle SUPIR user")
    void testSupirUser() {
        User supirUser = new User(
                userId,
                "supir",
                "supir@example.com",
                "Supir User",
                "hashed_password",
                UserRole.SUPIR,
                null,
                null,
                now,
                now
        );
        assertEquals(UserRole.SUPIR, supirUser.getRole());
        assertFalse(supirUser.isAdmin());
        assertFalse(supirUser.isOAuthAccount());
    }

    @Test
    @DisplayName("Should handle ADMIN user")
    void testAdminUser() {
        User adminUser = new User(
                userId,
                "admin",
                "admin@example.com",
                "Admin User",
                "hashed_password",
                UserRole.ADMIN,
                null,
                null,
                now,
                now
        );
        assertEquals(UserRole.ADMIN, adminUser.getRole());
        assertTrue(adminUser.isAdmin());
    }

    @Test
    @DisplayName("Should handle OAuth user with MANDOR role")
    void testOAuthUserWithMandorRole() {
        String certNumber = "OAUTH-CERT-001";
        User oauthMandor = new User(
                userId,
                "oauth_mandor",
                "oauth_mandor@example.com",
                "OAuth Mandor",
                null,  // OAuth account has no password
                UserRole.MANDOR,
                null,
                certNumber,
                now,
                now
        );
        assertTrue(oauthMandor.isOAuthAccount());
        assertEquals(UserRole.MANDOR, oauthMandor.getRole());
        assertEquals(certNumber, oauthMandor.getMandorCertificationNumber());
    }
}
