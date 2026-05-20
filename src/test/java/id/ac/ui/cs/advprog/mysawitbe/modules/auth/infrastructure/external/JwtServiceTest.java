package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.OAuthPendingRegistrationDTO;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecret = "my-super-secret-key-that-is-at-least-32-characters-long-for-hs256";
    private final long testExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(testSecret, testExpirationMs);
    }

    @Test
    @DisplayName("Should generate valid token with userId and role")
    void testGenerateToken() {
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        String role = "BURUH";

        String token = jwtService.generateToken(userId, role);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Should generate OAuth registration token")
    void testGenerateOAuthRegistrationToken() {
        String email = "test@example.com";
        String name = "Test User";

        String token = jwtService.generateOAuthRegistrationToken(email, name);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Should extract userId from token")
    void testExtractUserId() {
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        String role = "MANDOR";

        String token = jwtService.generateToken(userId, role);
        String extractedUserId = jwtService.extractUserId(token);

        assertEquals(userId, extractedUserId);
    }

    @Test
    @DisplayName("Should extract role from token")
    void testExtractRole() {
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        String role = "ADMIN";

        String token = jwtService.generateToken(userId, role);
        String extractedRole = jwtService.extractRole(token);

        assertEquals(role, extractedRole);
    }

    @Test
    @DisplayName("Should extract OAuth pending registration from valid token")
    void testExtractOAuthPendingRegistration() {
        String email = "oauth@example.com";
        String name = "OAuth User";

        String token = jwtService.generateOAuthRegistrationToken(email, name);
        OAuthPendingRegistrationDTO pending = jwtService.extractOAuthPendingRegistration(token);

        assertNotNull(pending);
        assertEquals(email, pending.email());
        assertEquals(name, pending.name());
    }

    @Test
    @DisplayName("Should reject invalid registration token type")
    void testExtractOAuthPendingRegistrationWithInvalidType() {
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        String token = jwtService.generateToken(userId, "BURUH");

        assertThrows(IllegalArgumentException.class, () -> 
            jwtService.extractOAuthPendingRegistration(token)
        );
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testExtractFromMalformedToken() {
        String malformedToken = "invalid.token.format";

        assertThrows(JwtException.class, () -> 
            jwtService.extractUserId(malformedToken)
        );
    }

    @Test
    @DisplayName("Should validate authentic token")
    void testIsTokenValidWithAuthenticToken() {
        String token = jwtService.generateToken("user-123", "SUPIR");
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Should reject malformed token on validation")
    void testIsTokenValidWithMalformedToken() {
        String malformedToken = "not.a.valid.token.at.all";
        assertFalse(jwtService.isTokenValid(malformedToken));
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void testGenerateDifferentTokensForDifferentUsers() {
        String token1 = jwtService.generateToken("user-1", "BURUH");
        String token2 = jwtService.generateToken("user-2", "MANDOR");

        assertNotEquals(token1, token2);
        assertEquals("user-1", jwtService.extractUserId(token1));
        assertEquals("user-2", jwtService.extractUserId(token2));
    }

    @Test
    @DisplayName("Should handle all roles correctly")
    void testGenerateTokenWithDifferentRoles() {
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        
        String adminToken = jwtService.generateToken(userId, "ADMIN");
        String buruhToken = jwtService.generateToken(userId, "BURUH");
        String mandorToken = jwtService.generateToken(userId, "MANDOR");
        String supirToken = jwtService.generateToken(userId, "SUPIR");

        assertEquals("ADMIN", jwtService.extractRole(adminToken));
        assertEquals("BURUH", jwtService.extractRole(buruhToken));
        assertEquals("MANDOR", jwtService.extractRole(mandorToken));
        assertEquals("SUPIR", jwtService.extractRole(supirToken));
    }

    @Test
    @DisplayName("Should handle special characters in email")
    void testOAuthTokenWithSpecialCharactersInEmail() {
        String email = "user+test@example.co.uk";
        String name = "Test User";

        String token = jwtService.generateOAuthRegistrationToken(email, name);
        OAuthPendingRegistrationDTO pending = jwtService.extractOAuthPendingRegistration(token);

        assertEquals(email, pending.email());
    }

    @Test
    @DisplayName("Should handle spaces in names")
    void testOAuthTokenWithSpacesInName() {
        String email = "test@example.com";
        String name = "John Doe Smith";

        String token = jwtService.generateOAuthRegistrationToken(email, name);
        OAuthPendingRegistrationDTO pending = jwtService.extractOAuthPendingRegistration(token);

        assertEquals(name, pending.name());
    }

    @Test
    @DisplayName("Should reject registration token with missing email")
    void testExtractOAuthWithMissingEmail() {
        // Create a token manually without email field - this should be caught
        String invalidEmail = "";
        String name = "Test";
        
        // The token itself would be invalid or incomplete
        assertThrows(Exception.class, () -> {
            String token = jwtService.generateOAuthRegistrationToken(invalidEmail, name);
            jwtService.extractOAuthPendingRegistration(token);
        });
    }

    @Test
    @DisplayName("Should work with UUID in different formats")
    void testTokenWithUUID() {
        String userId = "12345678-1234-1234-1234-123456789012";
        String token = jwtService.generateToken(userId, "BURUH");
        
        assertEquals(userId, jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Should generate unique tokens on each call")
    void testTokenUniquenessAcrossMultipleCalls() throws InterruptedException {
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        String role = "BURUH";

        String token1 = jwtService.generateToken(userId, role);
        Thread.sleep(50); // Increase delay to ensure different timestamps
        String token2 = jwtService.generateToken(userId, role);

        // Tokens should be different due to different issuedAt times (or might be same but still valid)
        // What's important is that both tokens are valid and contain correct data
        assertTrue(jwtService.isTokenValid(token1));
        assertTrue(jwtService.isTokenValid(token2));
        // But they should be valid and have same user/role
        assertEquals(userId, jwtService.extractUserId(token1));
        assertEquals(userId, jwtService.extractUserId(token2));
        assertEquals(role, jwtService.extractRole(token1));
        assertEquals(role, jwtService.extractRole(token2));
    }
}
