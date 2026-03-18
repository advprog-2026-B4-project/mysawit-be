package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2RedisAdapter")
class OAuth2RedisAdapterTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    OAuth2RedisAdapter adapter;

    private static final String STATE_PREFIX = "oauth2:state:";

    @BeforeEach
    void setUp() {
        adapter = new OAuth2RedisAdapter(redisTemplate, "google-client-id", "google-client-secret");
    }

    @Nested
    @DisplayName("generateState")
    class GenerateState {

        @Test
        @DisplayName("returns non-blank string")
        void returnsNonBlankString() {
            assertThat(adapter.generateState()).isNotBlank();
        }

        @Test
        @DisplayName("consecutive calls return different states")
        void consecutiveCallsAreUnique() {
            assertThat(adapter.generateState()).isNotEqualTo(adapter.generateState());
        }

        @Test
        @DisplayName("output is URL-safe base64 (no +, /, =)")
        void outputIsUrlSafe() {
            String state = adapter.generateState();
            assertThat(state).doesNotContain("+", "/", "=");
        }
    }

    @Nested
    @DisplayName("storeState")
    class StoreState {

        @BeforeEach
        void setUp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("returns true and stores key with correct prefix and TTL")
        void storesKeyWithPrefixAndTtl() {
            boolean result = adapter.storeState("my-state", Duration.ofMinutes(10));

            assertThat(result).isTrue();
            verify(valueOps).set(STATE_PREFIX + "my-state", "1", Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("Redis exception returns false instead of propagating")
        void redisExceptionReturnsFalse() {
            doThrow(new RuntimeException("Redis down"))
                    .when(valueOps).set(any(), any(), any(Duration.class));

            boolean result = adapter.storeState("state", Duration.ofMinutes(10));

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("validateAndConsumeState")
    class ValidateAndConsumeState {

        @Test
        @DisplayName("state exists – returns true and deletes key")
        void existingStateReturnsTrueAndDeletes() {
            when(redisTemplate.hasKey(STATE_PREFIX + "valid")).thenReturn(true);

            boolean result = adapter.validateAndConsumeState("valid");

            assertThat(result).isTrue();
            verify(redisTemplate).delete(STATE_PREFIX + "valid");
        }

        @Test
        @DisplayName("state does not exist – returns false, no delete")
        void missingStateReturnsFalseNoDelete() {
            when(redisTemplate.hasKey(STATE_PREFIX + "missing")).thenReturn(false);

            boolean result = adapter.validateAndConsumeState("missing");

            assertThat(result).isFalse();
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("hasKey returns null – treated as false, returns false")
        void nullHasKeyReturnsFalse() {
            when(redisTemplate.hasKey(STATE_PREFIX + "x")).thenReturn(null);

            assertThat(adapter.validateAndConsumeState("x")).isFalse();
        }
    }
}