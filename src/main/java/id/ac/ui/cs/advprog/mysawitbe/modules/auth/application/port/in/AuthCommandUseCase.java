package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.AuthTokenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.GoogleOAuthUrlDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;

import java.util.UUID;

/**
 * Use case interface for auth write operations.
 * Implemented in the application layer; called by the infrastructure web adapter.
 */
public interface AuthCommandUseCase {

    /**
     * Authenticate user with email/password.
     * Returns JWT token on success.
     */
    AuthTokenDTO loginWithEmail(String email, String password);

    /**
     * Register a new user with email/password.
     * Role must be one of: ADMIN, MANDOR, BURUH, SUPIR.
     * Returns the created user's data.
     */
    UserDTO registerUser(String email, String password, String name, String role);

    /**
     * Generate Google OAuth2 authorization URL with state parameter.
     * State is stored in Redis for CSRF validation.
     */
    GoogleOAuthUrlDTO getGoogleOAuthUrl();

    /**
     * Handle OAuth2 callback from Google.
     * Validates state, exchanges code for Google tokens, retrieves user info,
     * creates/updates user record, and issues JWT.
     */
    AuthTokenDTO handleGoogleOAuthCallback(String code, String state);

    /**
     * Invalidate current user session/token.
     */
    void logout(UUID userId);

    /**
     * Edit an existing user's details.
     * Admin-only; cannot edit another admin.
     */
    UserDTO editUser(UUID userId, String name, String role, String email);

    /**
     * Delete a user by ID.
     * Admin-only; cannot delete self.
     */
    void deleteUser(UUID requestingAdminId, UUID targetUserId);

    /**
     * Assign a buruh to a mandor.
     * Publishes BuruhAssignedEvent after successful assignment.
     */
    void assignBuruhToMandor(UUID buruhId, UUID mandorId);
}
