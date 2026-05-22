package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto;

/**
 * Response DTO containing Google OAuth2 authorization URL.
 * state: CSRF protection token; must be validated on callback.
 */
public record GoogleOAuthUrlDTO(
        String authorizationUrl,
        String state
) {}
