package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.OAuth2Port;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class OAuth2RedisAdapter implements OAuth2Port {

    private static final String STATE_PREFIX = "oauth2:state:";

    private final StringRedisTemplate  redisTemplate;
    private final WebClient            webClient;
    private final String               clientId;
    private final String               clientSecret;

    public OAuth2RedisAdapter(
            StringRedisTemplate redisTemplate,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret) {
        this.redisTemplate = redisTemplate;
        this.webClient     = WebClient.builder().build();
        this.clientId      = clientId;
        this.clientSecret  = clientSecret;
    }

    @Override
    public String generateState() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public boolean storeState(String state, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(STATE_PREFIX + state, "1", ttl);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean validateAndConsumeState(String state) {
        String key = STATE_PREFIX + state;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Object> exchangeCodeForTokens(String code, String redirectUri) {
        // 1. Exchange code for Google access token
        Map<?, ?> tokenResponse = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .bodyValue(Map.of(
                        "code",          code,
                        "client_id",     clientId,
                        "client_secret", clientSecret,
                        "redirect_uri",  redirectUri,
                        "grant_type",    "authorization_code"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new IllegalStateException("Failed to exchange OAuth2 code for token");
        }

        // 2. Fetch user info from Google
        String accessToken = (String) tokenResponse.get("access_token");
        Map<?, ?> userInfo = webClient.get()
                .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (userInfo == null) {
            throw new IllegalStateException("Failed to fetch user info from Google");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("access_token", accessToken);
        result.put("email",        userInfo.get("email"));
        result.put("name",         userInfo.get("name"));
        result.put("picture",      userInfo.get("picture"));
        return result;
    }
}