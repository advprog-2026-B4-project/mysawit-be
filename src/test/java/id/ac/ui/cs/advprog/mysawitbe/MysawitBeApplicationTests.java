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
    "DB_URL=jdbc:h2:mem:mysawit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "DB_USERNAME=sa",
    "DB_PASSWORD=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "REDIS_HOST=localhost",
    "REDIS_PORT=6379",
    "REDIS_PASSWORD="
})
class MysawitBeApplicationTests {

    @Test
    void contextLoads() {
    }
}
