package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private OAuth2RedisAdapter oauth2RedisAdapter;

    @BeforeEach
    void setUp() {
        oauth2RedisAdapter = new OAuth2RedisAdapter(redisTemplate, "test-client-id", "test-client-secret");
        ReflectionTestUtils.setField(oauth2RedisAdapter, "webClient", webClient);
    }

    private void setupWebClientMocks(Map<String, Object> tokenResponse, Map<String, Object> userInfoResponse) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).bodyValue(any());
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        doReturn(requestHeadersSpec).when(requestHeadersSpec).header(anyString(), anyString());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(tokenResponse))
                .thenReturn(Mono.just(userInfoResponse));
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
    @DisplayName("Should validate and consume valid state")
    void testValidateAndConsumeValidState() {
        String state = "valid-state";
        String key = "oauth2:state:" + state;

        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(redisTemplate.delete(key)).thenReturn(Boolean.TRUE);

        boolean result = oauth2RedisAdapter.validateAndConsumeState(state);

        assertTrue(result);
        verify(redisTemplate).hasKey(key);
        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("Should exchange code for tokens successfully")
    void testExchangeCodeForTokensSuccess() {
        String code = "auth_code";
        String redirectUri = "http://localhost:8080/callback";
        String accessToken = "test_access_token";
        String email = "user@example.com";
        String name = "Test User";

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", accessToken);
        tokenResponse.put("token_type", "Bearer");

        Map<String, Object> userInfoResponse = new HashMap<>();
        userInfoResponse.put("email", email);
        userInfoResponse.put("name", name);
        userInfoResponse.put("picture", "https://example.com/pic.jpg");

        setupWebClientMocks(tokenResponse, userInfoResponse);

        Map<String, Object> result = oauth2RedisAdapter.exchangeCodeForTokens(code, redirectUri);

        assertNotNull(result);
        assertEquals(accessToken, result.get("access_token"));
        assertEquals(email, result.get("email"));
        assertEquals(name, result.get("name"));
        assertEquals("https://example.com/pic.jpg", result.get("picture"));
    }

    @Test
    @DisplayName("Should handle missing picture field in user info")
    void testExchangeCodeForTokensMissingPicture() {
        String code = "auth-code-123";
        String redirectUri = "http://localhost:8080/callback";
        String accessToken = "access-token-xyz";
        String email = "user@example.com";
        String name = "Test User";

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", accessToken);

        Map<String, Object> userInfoResponse = new HashMap<>();
        userInfoResponse.put("email", email);
        userInfoResponse.put("name", name);

        setupWebClientMocks(tokenResponse, userInfoResponse);

        Map<String, Object> result = oauth2RedisAdapter.exchangeCodeForTokens(code, redirectUri);

        assertNotNull(result);
        assertNull(result.get("picture"));
        assertEquals(email, result.get("email"));
    }
}
