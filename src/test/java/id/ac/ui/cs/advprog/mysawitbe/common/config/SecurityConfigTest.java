package id.ac.ui.cs.advprog.mysawitbe.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000",
        "app.jwt.secret=my-test-secret-key-that-is-long-enough-for-hmac-sha256-minimum-32-bytes",
        "app.jwt.expiration-ms=3600000",
        "app.midtrans.server-key=sk-test",
        "app.midtrans.client-key=ck-test",
        "app.midtrans.merchant-id=M001",
        "app.midtrans.snap-base-url=https://sandbox.midtrans.com",
        "app.midtrans.notification-url=https://test.com/callback",
        "app.midtrans.redirect-url=https://test.com/status",
        "app.storage.r2.endpoint=https://test.r2.dev",
        "app.storage.r2.bucket=test",
        "app.storage.r2.access-key=test",
        "app.storage.r2.secret-key=test",
        "app.storage.r2.public-url=https://test.r2.dev",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
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
