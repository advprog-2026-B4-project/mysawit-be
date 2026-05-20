package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2RedisAdapter Tests")
class OAuth2RedisAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OAuth2RedisAdapter oauth2RedisAdapter;
    private final String clientId = "test-client-id";
    private final String clientSecret = "test-client-secret";

    @BeforeEach
    void setUp() {
        oauth2RedisAdapter = new OAuth2RedisAdapter(redisTemplate, clientId, clientSecret);
    }

    @Test
    @DisplayName("Should generate random state token")
    void testGenerateState() {
        String state = oauth2RedisAdapter.generateState();
        assertNotNull(state);
        assertFalse(state.isEmpty());
        assertTrue(state.length() > 10);
    }

    @Test
    @DisplayName("Should generate different states on each call")
    void testGenerateStateUniqueness() {
        String state1 = oauth2RedisAdapter.generateState();
        String state2 = oauth2RedisAdapter.generateState();
        assertNotEquals(state1, state2);
    }

    @Test
    @DisplayName("Should store state successfully")
    void testStoreStateSuccess() {
        String state = "test-state-12345";
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        boolean result = oauth2RedisAdapter.storeState(state, ttl);

        assertTrue(result);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set("oauth2:state:" + state, "1", ttl);
    }

    @Test
    @DisplayName("Should validate and consume valid state")
    void testValidateAndConsumeValidState() {
        String state = "valid-state";

        when(redisTemplate.hasKey("oauth2:state:" + state)).thenReturn(true);

        boolean result = oauth2RedisAdapter.validateAndConsumeState(state);

        assertTrue(result);
        verify(redisTemplate).hasKey("oauth2:state:" + state);
        verify(redisTemplate).delete("oauth2:state:" + state);
    }

    @Test
    @DisplayName("Should reject invalid state")
    void testValidateAndConsumeInvalidState() {
        String state = "invalid-state";

        when(redisTemplate.hasKey("oauth2:state:" + state)).thenReturn(false);

        boolean result = oauth2RedisAdapter.validateAndConsumeState(state);

        assertFalse(result);
        verify(redisTemplate).hasKey("oauth2:state:" + state);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should reject state that was already consumed")
    void testValidateAndConsumeAlreadyConsumedState() {
        String state = "already-consumed-state";

        when(redisTemplate.hasKey("oauth2:state:" + state)).thenReturn(false);

        boolean result = oauth2RedisAdapter.validateAndConsumeState(state);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle store state exception gracefully")
    void testStoreStateWithException() {
        String state = "test-state";
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection error"));

        boolean result = oauth2RedisAdapter.storeState(state, ttl);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should generate state with sufficient entropy")
    void testStateEntropy() {
        java.util.Set<String> states = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            states.add(oauth2RedisAdapter.generateState());
        }
        assertEquals(100, states.size(), "States should be unique");
    }

    @Test
    @DisplayName("Should handle multiple state validations")
    void testMultipleStateValidations() {
        String state1 = "state-1";
        when(redisTemplate.hasKey("oauth2:state:" + state1)).thenReturn(true);
        
        boolean result1 = oauth2RedisAdapter.validateAndConsumeState(state1);
        assertTrue(result1);

        String state2 = "state-2";
        when(redisTemplate.hasKey("oauth2:state:" + state2)).thenReturn(true);
        
        boolean result2 = oauth2RedisAdapter.validateAndConsumeState(state2);
        assertTrue(result2);
    }

    @Test
    @DisplayName("Should store state with different TTLs")
    void testStoreDifferentTTLs() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        String state1 = "state-5min";
        oauth2RedisAdapter.storeState(state1, Duration.ofMinutes(5));
        verify(valueOperations).set("oauth2:state:" + state1, "1", Duration.ofMinutes(5));

        String state2 = "state-10min";
        oauth2RedisAdapter.storeState(state2, Duration.ofMinutes(10));
        verify(valueOperations).set("oauth2:state:" + state2, "1", Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("Should validate state with special characters in key")
    void testValidateStateWithSpecialCharacters() {
        String state = "state-with-special-_-chars";

        when(redisTemplate.hasKey("oauth2:state:" + state)).thenReturn(true);

        boolean result = oauth2RedisAdapter.validateAndConsumeState(state);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should generate state with URL-safe base64 encoding")
    void testGenerateStateFormat() {
        String state = oauth2RedisAdapter.generateState();
        
        assertFalse(state.contains("+"), "State should not contain + character");
        assertFalse(state.contains("/"), "State should not contain / character");
        assertFalse(state.contains("="), "State should not contain padding");
    }

    @Test
    @DisplayName("Should store state multiple times with success")
    void testStoreStateMultipleTimes() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        for (int i = 0; i < 5; i++) {
            String state = "state-" + i;
            boolean result = oauth2RedisAdapter.storeState(state, Duration.ofMinutes(10));
            assertTrue(result);
        }

        verify(valueOperations, times(5)).set(anyString(), eq("1"), any(Duration.class));
    }

    @Test
    @DisplayName("Should handle state key prefix correctly")
    void testStateKeyPrefixHandling() {
        String state = "test-state-prefix";
        when(redisTemplate.hasKey(contains(state))).thenReturn(true);

        oauth2RedisAdapter.validateAndConsumeState(state);

        verify(redisTemplate).hasKey("oauth2:state:" + state);
    }

    @Test
    @DisplayName("Should return false when state validation hasKey returns null")
    void testValidateStateWithNullFromRedis() {
        String state = "null-state";

        when(redisTemplate.hasKey("oauth2:state:" + state)).thenReturn(null);

        boolean result = oauth2RedisAdapter.validateAndConsumeState(state);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle store state with empty state string")
    void testStoreStateWithEmptyString() {
        String state = "";
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        boolean result = oauth2RedisAdapter.storeState(state, ttl);

        assertTrue(result);
    }
}

