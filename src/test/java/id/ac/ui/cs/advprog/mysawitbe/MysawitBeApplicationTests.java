package id.ac.ui.cs.advprog.mysawitbe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "GOOGLE_CLIENT_ID=test-client-id",
    "GOOGLE_CLIENT_SECRET=test-client-secret",
    "JWT_SECRET=test-secret-min-256-bits-for-testing-only-padding",
    "JWT_EXPIRATION_MS=86400000",
    "DB_URL=jdbc:postgresql://localhost:5432/mysawit",
    "DB_USERNAME=postgres",
    "DB_PASSWORD=postgres",
    "REDIS_HOST=localhost",
    "REDIS_PORT=6379",
    "REDIS_PASSWORD="
})
class MysawitBeApplicationTests {

    @Test
    void contextLoads() {
    }
}