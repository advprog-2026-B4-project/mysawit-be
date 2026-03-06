package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto;

/**
 * Request DTO for handling Google OAuth2 callback.
 * code: Authorization code from Google.
 * state: CSRF token that must match the one generated in the authorization request.
 */
public record GoogleOAuthCallbackDTO(
        String code,
        String state
) {}
