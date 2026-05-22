package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out;

import java.time.Duration;

/**
 * Outbound port for OAuth2-related operations (state management, token storage).
 * Implemented by infrastructure/external/OAuth2RedisAdapter or similar.
 */
public interface OAuth2Port {

    /**
     * Generate a secure random state token for CSRF protection.
     */
    String generateState();

    /**
     * Store state token in Redis with expiration (typically 10 minutes).
     * Returns true if successfully stored.
     */
    boolean storeState(String state, Duration ttl);

    /**
     * Validate and consume state token (remove from Redis after validation).
     * Returns true if state exists and is valid.
     */
    boolean validateAndConsumeState(String state);

    /**
     * Exchange authorization code for Google access token and user info.
     * Returns a map containing: access_token, id_token, email, name, picture.
     */
    java.util.Map<String, Object> exchangeCodeForTokens(String code, String redirectUri);
}
