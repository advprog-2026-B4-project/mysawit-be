package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET    = "test-secret-key-32-chars-minimum!";
    private static final long   EXPIRY_MS = 3_600_000L; // 1 hour
    private static final long   EXPIRED_MS = 1L;        // instantly expires

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRY_MS);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("returns non-blank token string")
        void returnsNonBlankToken() {
            assertThat(jwtService.generateToken("some-id", "BURUH")).isNotBlank();
        }

        @Test
        @DisplayName("different inputs produce different tokens")
        void uniqueTokensPerInput() {
            String t1 = jwtService.generateToken(UUID.randomUUID().toString(), "BURUH");
            String t2 = jwtService.generateToken(UUID.randomUUID().toString(), "BURUH");
            assertThat(t1).isNotEqualTo(t2);
        }
    }

    @Nested
    @DisplayName("extractUserId")
    class ExtractUserId {

        @Test
        @DisplayName("extracted subject matches input userId")
        void extractedSubjectMatchesInput() {
            String userId = UUID.randomUUID().toString();
            String token  = jwtService.generateToken(userId, "ADMIN");
            assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("extracted role matches input role")
        void extractedRoleMatchesInput() {
            String token = jwtService.generateToken("id-123", "MANDOR");
            assertThat(jwtService.extractRole(token)).isEqualTo("MANDOR");
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("fresh token is valid")
        void freshTokenIsValid() {
            String token = jwtService.generateToken("id", "BURUH");
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("expired token is invalid")
        void expiredTokenIsInvalid() throws InterruptedException {
            JwtService shortLived = new JwtService(SECRET, EXPIRED_MS);
            String token = shortLived.generateToken("id", "BURUH");
            Thread.sleep(10);
            assertThat(shortLived.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("malformed string is invalid")
        void malformedTokenIsInvalid() {
            assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
        }

        @Test
        @DisplayName("empty string is invalid")
        void emptyStringIsInvalid() {
            assertThat(jwtService.isTokenValid("")).isFalse();
        }

        @Test
        @DisplayName("token signed with different secret is invalid")
        void wrongSecretIsInvalid() {
            JwtService other = new JwtService("other-secret-key-32-chars-min!!!!", EXPIRY_MS);
            String token = other.generateToken("id", "BURUH");
            assertThat(jwtService.isTokenValid(token)).isFalse();
        }
    }

    @Test
    @DisplayName("generate → extract round-trip preserves all claims")
    void roundTripPreservesAllClaims() {
        String userId = UUID.randomUUID().toString();
        String role   = "SUPIR";
        String token  = jwtService.generateToken(userId, role);

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractRole(token)).isEqualTo(role);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }
}