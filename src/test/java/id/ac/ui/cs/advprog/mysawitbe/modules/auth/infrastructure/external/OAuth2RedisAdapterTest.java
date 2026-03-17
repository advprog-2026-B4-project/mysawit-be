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

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2RedisAdapter")
class OAuth2RedisAdapterTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    OAuth2RedisAdapter adapter;

    @BeforeEach
    void setUp() {
        // TODO: wire mocks and instantiate adapter
    }

    @Nested
    @DisplayName("generateState")
    class GenerateState {

        @Test
        @DisplayName("returns non-blank string")
        void returnsNonBlankString() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("consecutive calls return different states")
        void consecutiveCallsAreUnique() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("output is URL-safe base64 (no +, /, =)")
        void outputIsUrlSafe() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("storeState")
    class StoreState {

        @Test
        @DisplayName("returns true and stores key with correct prefix and TTL")
        void storesKeyWithPrefixAndTtl() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("Redis exception returns false instead of propagating")
        void redisExceptionReturnsFalse() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("validateAndConsumeState")
    class ValidateAndConsumeState {

        @Test
        @DisplayName("state exists returns true and deletes key")
        void existingStateReturnsTrueAndDeletes() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("state does not exist returns false, no delete")
        void missingStateReturnsFalseNoDelete() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("hasKey returns null treated as false")
        void nullHasKeyReturnsFalse() {
            fail("not yet implemented");
        }
    }
}