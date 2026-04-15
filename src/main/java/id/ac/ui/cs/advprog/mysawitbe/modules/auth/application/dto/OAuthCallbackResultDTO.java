package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto;

/**
 * Result DTO for Google OAuth callback.
 * Existing users receive an access token, new users receive a registration token.
 */
public record OAuthCallbackResultDTO(
        String accessToken,
        String role,
        String registrationToken,
        String email,
        String name,
        boolean registrationRequired
) {
    public static OAuthCallbackResultDTO authenticated(String accessToken, String role) {
        return new OAuthCallbackResultDTO(accessToken, role, null, null, null, false);
    }

    public static OAuthCallbackResultDTO registrationRequired(String registrationToken, String email, String name) {
        return new OAuthCallbackResultDTO(null, null, registrationToken, email, name, true);
    }
}
