package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto;

/**
 * Response DTO containing JWT access token after successful authentication.
 * Used for both email/password login and OAuth2 login flows.
 */
public record AuthTokenDTO(
        String accessToken,
        String tokenType,
        String role
) {
    public AuthTokenDTO(String accessToken, String role) {
        this(accessToken, "Bearer", role);
    }
}
