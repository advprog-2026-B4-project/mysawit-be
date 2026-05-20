package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto;

/**
 * Temporary payload embedded in OAuth signup token for users that have not completed registration.
 */
public record OAuthPendingRegistrationDTO(
        String email,
        String name
) {
}
