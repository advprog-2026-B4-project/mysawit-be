package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("JwtService")
class JwtServiceTest {

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        // TODO: initialise JwtService with test secret and expiry
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("returns non-blank token string")
        void returnsNonBlankToken() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("different inputs produce different tokens")
        void uniqueTokensPerInput() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("extractUserId")
    class ExtractUserId {

        @Test
        @DisplayName("extracted subject matches input userId")
        void extractedSubjectMatchesInput() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("extracted role matches input role")
        void extractedRoleMatchesInput() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("fresh token is valid")
        void freshTokenIsValid() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("expired token is invalid")
        void expiredTokenIsInvalid() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("malformed string is invalid")
        void malformedTokenIsInvalid() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("empty string is invalid")
        void emptyStringIsInvalid() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("token signed with different secret is invalid")
        void wrongSecretIsInvalid() {
            fail("not yet implemented");
        }
    }

    @Test
    @DisplayName("generate → extract round-trip preserves all claims")
    void roundTripPreservesAllClaims() {
        fail("not yet implemented");
    }
}