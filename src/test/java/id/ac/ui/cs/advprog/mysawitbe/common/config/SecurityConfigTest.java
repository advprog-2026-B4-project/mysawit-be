package id.ac.ui.cs.advprog.mysawitbe.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Test
    void passwordEncoder_isNotNull() {
        assertThat(passwordEncoder).isNotNull();
    }

    @Nested
    class PasswordEncoding {

        @Test
        void passwordEncoder_isBCrypt() {
            String encoded = passwordEncoder.encode("test-password");
            assertThat(encoded).startsWith("$2a$12$");
        }

        @Test
        void passwordEncoder_matchesCorrectPassword() {
            String encoded = passwordEncoder.encode("myPassword123");
            assertThat(passwordEncoder.matches("myPassword123", encoded)).isTrue();
        }

        @Test
        void passwordEncoder_rejectsWrongPassword() {
            String encoded = passwordEncoder.encode("myPassword123");
            assertThat(passwordEncoder.matches("wrongPassword", encoded)).isFalse();
        }

        @Test
        void passwordEncoder_generatesDifferentHashesForSameInput() {
            String hash1 = passwordEncoder.encode("samePassword");
            String hash2 = passwordEncoder.encode("samePassword");
            assertThat(hash1).isNotEqualTo(hash2);
        }
    }
}
