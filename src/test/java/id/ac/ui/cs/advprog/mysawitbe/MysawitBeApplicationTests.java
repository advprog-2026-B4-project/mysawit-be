package id.ac.ui.cs.advprog.mysawitbe;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
    "REDIS_PASSWORD=",
    "spring.security.oauth2.client.registration.google.client-id=test-client-id",
    "spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
    "app.google.redirect-uri=http://localhost:8080/api/auth/oauth2/callback",
    "app.frontend.url=http://localhost:3000",
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
        "org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration"
})
class MysawitBeApplicationTests {

    // OAuth2RedisAdapter depends on StringRedisTemplate — provide a mock so no real Redis needed
    @MockitoBean
    StringRedisTemplate stringRedisTemplate;

    // RateLimitFilter + RedisRateLimitConfig depend on this — mock prevents eager Lettuce connect
    @MockitoBean
    @SuppressWarnings("rawtypes")
    LettuceBasedProxyManager rateLimitProxyManager;

    // CacheConfig.cacheManager() and cacheEvictor depend on CacheManager — mock skips RedisCacheManager creation
    @MockitoBean
    CacheManager cacheManager;

    @Test
    void contextLoads() {
        // Spring context loads successfully if no exception is thrown before reaching this point.
        // All bean wiring, configuration, and autowiring is validated by the framework.
    }
}
