package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.*;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.event.BuruhAssignedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.OAuth2Port;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.UserRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import id.ac.ui.cs.advprog.mysawitbe.common.port.DomainEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthCommandUseCaseImpl Tests")
class AuthCommandUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private OAuth2Port oauth2Port;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<BuruhAssignedEvent> eventCaptor;

    private AuthCommandUseCaseImpl authCommandUseCase;
    private UUID userId;
    private UUID mandorId;
    private String googleClientId = "test-client-id";
    private String googleRedirectUri = "http://localhost:8080/auth/callback";

    @BeforeEach
    void setUp() {
        authCommandUseCase = new AuthCommandUseCaseImpl(
                userRepository,
                oauth2Port,
                jwtService,
                passwordEncoder,
                eventPublisher,
                googleClientId,
                googleRedirectUri
        );
        userId = UUID.randomUUID();
        mandorId = UUID.randomUUID();
    }

    // ===================== LOGIN TESTS =====================

    @Test
    @DisplayName("Should login user with correct email and password")
    void testLoginWithEmailSuccess() {
        String email = "test@example.com";
        String password = "password123";
        String hashedPassword = "hashed_password";
        String token = "jwt_token";

        UserDTO userDTO = new UserDTO(userId, "testuser", "Test User", "BURUH", email, null, null);

        when(userRepository.findPasswordHashByEmail(email)).thenReturn(hashedPassword);
        when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(userDTO);
        when(jwtService.generateToken(userId.toString(), "BURUH")).thenReturn(token);

        AuthTokenDTO result = authCommandUseCase.loginWithEmail(email, password);

        assertNotNull(result);
        assertEquals(token, result.accessToken());
        assertEquals("BURUH", result.role());
        verify(userRepository, times(1)).findPasswordHashByEmail(email);
        verify(passwordEncoder, times(1)).matches(password, hashedPassword);
        verify(jwtService, times(1)).generateToken(userId.toString(), "BURUH");
    }

    @Test
    @DisplayName("Should throw exception for non-existent email")
    void testLoginWithEmailNotFound() {
        String email = "nonexistent@example.com";
        String password = "password123";

        when(userRepository.findPasswordHashByEmail(email)).thenReturn(null);
        when(userRepository.findByEmail(email)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.loginWithEmail(email, password)
        );
    }

    @Test
    @DisplayName("Should throw exception for OAuth-only account on password login")
    void testLoginWithEmailOAuthAccount() {
        String email = "oauth@example.com";
        String password = "password123";

        UserDTO oauthUser = new UserDTO(userId, "oauthuser", "OAuth User", "BURUH", email, null, null);

        when(userRepository.findPasswordHashByEmail(email)).thenReturn(null);
        when(userRepository.findByEmail(email)).thenReturn(oauthUser);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.loginWithEmail(email, password)
        );
        assertTrue(exception.getMessage().contains("Google"));
    }

    @Test
    @DisplayName("Should throw exception for incorrect password")
    void testLoginWithEmailWrongPassword() {
        String email = "test@example.com";
        String password = "wrongpassword";
        String hashedPassword = "hashed_password";

        when(userRepository.findPasswordHashByEmail(email)).thenReturn(hashedPassword);
        when(passwordEncoder.matches(password, hashedPassword)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.loginWithEmail(email, password)
        );
    }

    // ===================== REGISTRATION TESTS =====================

    @Test
    @DisplayName("Should register user with valid data")
    void testRegisterUserSuccess() {
        String email = "newuser@example.com";
        String password = "password123";
        String name = "New User";
        String role = "BURUH";
        String hashedPassword = "hashed_password_123";

        UserDTO savedUser = new UserDTO(userId, "newuser", name, role, email, null, null);

        when(userRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
        when(userRepository.save(any(UserDTO.class), eq(hashedPassword))).thenReturn(savedUser);

        UserDTO result = authCommandUseCase.registerUser(email, password, name, role, null);

        assertNotNull(result);
        assertEquals(name, result.name());
        assertEquals(role, result.role());
        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, times(1)).encode(password);
        verify(userRepository, times(1)).save(any(UserDTO.class), eq(hashedPassword));
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void testRegisterUserEmailExists() {
        String email = "existing@example.com";
        String password = "password123";
        UserDTO existingUser = new UserDTO(UUID.randomUUID(), "existing", "Existing User", "BURUH", email, null, null);

        when(userRepository.findByEmail(email)).thenReturn(existingUser);

        assertThrows(IllegalStateException.class, () ->
                authCommandUseCase.registerUser(email, password, "New User", "BURUH", null)
        );
    }

    @Test
    @DisplayName("Should throw exception when registering as ADMIN")
    void testRegisterUserAsAdmin() {
        String email = "admin@example.com";

        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.registerUser(email, "password", "Admin", "ADMIN", null)
        );
    }

    @Test
    @DisplayName("Should throw exception when registering MANDOR without certification")
    void testRegisterMandorWithoutCertification() {
        String email = "mandor@example.com";

        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.registerUser(email, "password", "Mandor", "MANDOR", null)
        );
    }

    @Test
    @DisplayName("Should register MANDOR with valid certification")
    void testRegisterMandorWithCertification() {
        String email = "mandor@example.com";
        String password = "password123";
        String name = "Mandor User";
        String role = "MANDOR";
        String certNumber = "CERT-12345";
        String hashedPassword = "hashed_password";

        UserDTO savedUser = new UserDTO(userId, "mandor", name, role, email, certNumber, null);

        when(userRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
        when(userRepository.save(any(UserDTO.class), eq(hashedPassword))).thenReturn(savedUser);

        UserDTO result = authCommandUseCase.registerUser(email, password, name, role, certNumber);

        assertNotNull(result);
        assertEquals(certNumber, result.mandorCertificationNumber());
        verify(userRepository, times(1)).save(any(UserDTO.class), eq(hashedPassword));
    }

    @Test
    @DisplayName("Should normalize role to uppercase")
    void testRegisterUserRoleNormalization() {
        String email = "test@example.com";
        String password = "password123";
        String name = "Test User";
        String role = "buruh";  // lowercase

        UserDTO savedUser = new UserDTO(userId, "test", name, "BURUH", email, null, null);

        when(userRepository.findByEmail(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("hashed");
        when(userRepository.save(any(UserDTO.class), anyString())).thenReturn(savedUser);

        UserDTO result = authCommandUseCase.registerUser(email, password, name, role, null);

        assertEquals("BURUH", result.role());
    }

    // ===================== OAUTH TESTS =====================

    @Test
    @DisplayName("Should generate Google OAuth URL")
    void testGetGoogleOAuthUrl() {
        String state = "random-state-123";

        when(oauth2Port.generateState()).thenReturn(state);
        when(oauth2Port.storeState(eq(state), any(Duration.class))).thenReturn(true);

        GoogleOAuthUrlDTO result = authCommandUseCase.getGoogleOAuthUrl();

        assertNotNull(result);
        assertNotNull(result.authorizationUrl());
        assertEquals(state, result.state());
        assertTrue(result.authorizationUrl().contains(googleClientId));
        verify(oauth2Port, times(1)).generateState();
        verify(oauth2Port, times(1)).storeState(eq(state), any(Duration.class));
    }

    @Test
    @DisplayName("Should handle Google OAuth callback for existing user")
    void testHandleGoogleOAuthCallbackExistingUser() {
        String code = "google-auth-code";
        String state = "valid-state";
        String email = "oauth@example.com";
        String name = "OAuth User";
        String token = "jwt_token";

        UserDTO existingUser = new UserDTO(userId, "oauthuser", name, "BURUH", email, null, null);
        Map<String, Object> tokens = Map.of(
                "email", email,
                "name", name
        );

        when(oauth2Port.validateAndConsumeState(state)).thenReturn(true);
        when(oauth2Port.exchangeCodeForTokens(code, googleRedirectUri)).thenReturn(tokens);
        when(userRepository.findByEmail(email)).thenReturn(existingUser);
        when(jwtService.generateToken(userId.toString(), "BURUH")).thenReturn(token);

        OAuthCallbackResultDTO result = authCommandUseCase.handleGoogleOAuthCallback(code, state);

        assertNotNull(result);
        assertEquals(token, result.accessToken());
        assertEquals("BURUH", result.role());
        assertFalse(result.registrationRequired());
        verify(oauth2Port, times(1)).validateAndConsumeState(state);
    }

    @Test
    @DisplayName("Should handle Google OAuth callback for new user")
    void testHandleGoogleOAuthCallbackNewUser() {
        String code = "google-auth-code";
        String state = "valid-state";
        String email = "newuser@example.com";
        String name = "New User";
        String registrationToken = "registration_token";

        Map<String, Object> tokens = Map.of(
                "email", email,
                "name", name
        );

        when(oauth2Port.validateAndConsumeState(state)).thenReturn(true);
        when(oauth2Port.exchangeCodeForTokens(code, googleRedirectUri)).thenReturn(tokens);
        when(userRepository.findByEmail(email)).thenReturn(null);
        when(jwtService.generateOAuthRegistrationToken(email, name)).thenReturn(registrationToken);

        OAuthCallbackResultDTO result = authCommandUseCase.handleGoogleOAuthCallback(code, state);

        assertNotNull(result);
        assertEquals(registrationToken, result.registrationToken());
        assertEquals(email, result.email());
        assertEquals(name, result.name());
        assertTrue(result.registrationRequired());
    }

    @Test
    @DisplayName("Should reject invalid OAuth state")
    void testHandleGoogleOAuthCallbackInvalidState() {
        String code = "google-auth-code";
        String state = "invalid-state";

        when(oauth2Port.validateAndConsumeState(state)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.handleGoogleOAuthCallback(code, state)
        );
    }

    @Test
    @DisplayName("Should reject incomplete user info from OAuth")
    void testHandleGoogleOAuthCallbackIncompleteInfo() {
        String code = "google-auth-code";
        String state = "valid-state";

        Map<String, Object> tokens = Map.of(
                "email", "",  // empty email
                "name", "User"
        );

        when(oauth2Port.validateAndConsumeState(state)).thenReturn(true);
        when(oauth2Port.exchangeCodeForTokens(code, googleRedirectUri)).thenReturn(tokens);

        assertThrows(IllegalStateException.class, () ->
                authCommandUseCase.handleGoogleOAuthCallback(code, state)
        );
    }

    @Test
    @DisplayName("Should complete OAuth registration successfully")
    void testCompleteGoogleOAuthRegistrationSuccess() {
        String registrationToken = "registration_token";
        String email = "oauth@example.com";
        String name = "OAuth User";
        String role = "BURUH";
        String token = "jwt_token";

        OAuthPendingRegistrationDTO pending = new OAuthPendingRegistrationDTO(email, name);
        UserDTO savedUser = new UserDTO(userId, "oauth", name, role, email, null, null);

        when(jwtService.extractOAuthPendingRegistration(registrationToken)).thenReturn(pending);
        when(userRepository.findByEmail(email)).thenReturn(null);
        when(userRepository.save(any(UserDTO.class), isNull())).thenReturn(savedUser);
        when(jwtService.generateToken(userId.toString(), role)).thenReturn(token);

        AuthTokenDTO result = authCommandUseCase.completeGoogleOAuthRegistration(registrationToken, role, null);

        assertNotNull(result);
        assertEquals(token, result.accessToken());
        assertEquals(role, result.role());
    }

    @Test
    @DisplayName("Should reject invalid OAuth registration token")
    void testCompleteGoogleOAuthRegistrationInvalidToken() {
        String registrationToken = "invalid_token";

        when(jwtService.extractOAuthPendingRegistration(registrationToken))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.completeGoogleOAuthRegistration(registrationToken, "BURUH", null)
        );
    }

    // ===================== EDIT USER TESTS =====================

    @Test
    @DisplayName("Should edit user successfully")
    void testEditUserSuccess() {
        String newName = "Updated Name";
        String newRole = "MANDOR";
        String newEmail = "newemail@example.com";
        String certNumber = "CERT-001";

        UserDTO existingUser = new UserDTO(userId, "testuser", "Test User", "BURUH", "test@example.com", null, null);
        UserDTO updatedUser = new UserDTO(userId, "testuser", newName, newRole, newEmail, certNumber, null);

        when(userRepository.findById(userId)).thenReturn(existingUser);
        when(userRepository.findPasswordHashByEmail("test@example.com")).thenReturn("hashed");
        when(userRepository.save(any(UserDTO.class), anyString())).thenReturn(updatedUser);

        UserDTO result = authCommandUseCase.editUser(userId, newName, newRole, newEmail, certNumber);

        assertNotNull(result);
        assertEquals(newName, result.name());
        assertEquals(newRole, result.role());
        assertEquals(newEmail, result.email());
        assertEquals(certNumber, result.mandorCertificationNumber());
    }

    @Test
    @DisplayName("Should throw exception when editing non-existent user")
    void testEditUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () ->
                authCommandUseCase.editUser(userId, "New Name", "BURUH", "new@example.com", null)
        );
    }

    @Test
    @DisplayName("Should throw exception when editing ADMIN account")
    void testEditAdminUser() {
        UserDTO adminUser = new UserDTO(userId, "admin", "Admin User", "ADMIN", "admin@example.com", null, null);

        when(userRepository.findById(userId)).thenReturn(adminUser);

        assertThrows(IllegalStateException.class, () ->
                authCommandUseCase.editUser(userId, "New Name", "ADMIN", "admin@example.com", null)
        );
    }

    // ===================== DELETE USER TESTS =====================

    @Test
    @DisplayName("Should delete user successfully")
    void testDeleteUserSuccess() {
        UUID adminId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(true);

        authCommandUseCase.deleteUser(adminId, userId);

        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    @DisplayName("Should throw exception when admin tries to delete themselves")
    void testDeleteUserSelfDelete() {
        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.deleteUser(userId, userId)
        );
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void testDeleteUserNotFound() {
        UUID adminId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () ->
                authCommandUseCase.deleteUser(adminId, userId)
        );
    }

    // ===================== BURUH-MANDOR ASSIGNMENT TESTS =====================

    @Test
    @DisplayName("Should assign buruh to mandor successfully")
    void testAssignBuruhToMandorSuccess() {
        UUID buruhId = UUID.randomUUID();
        UserDTO buruh = new UserDTO(buruhId, "buruh", "Buruh User", "BURUH", "buruh@example.com", null, null);
        UserDTO mandor = new UserDTO(mandorId, "mandor", "Mandor User", "MANDOR", "mandor@example.com", "CERT-001", null);

        when(userRepository.findById(buruhId)).thenReturn(buruh);
        when(userRepository.findById(mandorId)).thenReturn(mandor);

        authCommandUseCase.assignBuruhToMandor(buruhId, mandorId);

        verify(userRepository, times(1)).saveBuruhMandorAssignment(buruhId, mandorId);
        verify(eventPublisher, times(1)).publish(any(BuruhAssignedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when assigning non-BURUH user")
    void testAssignNonBuruhUser() {
        UUID mandorUserId = UUID.randomUUID();
        UserDTO mandor1 = new UserDTO(mandorUserId, "mandor", "Mandor User", "MANDOR", "mandor@example.com", "CERT-001", null);

        when(userRepository.findById(mandorUserId)).thenReturn(mandor1);
        when(userRepository.findById(mandorId)).thenReturn(new UserDTO(mandorId, "mandor2", "Mandor 2", "MANDOR", "mandor2@example.com", "CERT-002", null));

        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.assignBuruhToMandor(mandorUserId, mandorId)
        );
    }

    @Test
    @DisplayName("Should throw exception when assigning to non-MANDOR user")
    void testAssignToNonMandorUser() {
        UUID buruhId = UUID.randomUUID();
        UUID buruhUserId = UUID.randomUUID();
        UserDTO buruh = new UserDTO(buruhId, "buruh", "Buruh User", "BURUH", "buruh@example.com", null, null);
        UserDTO buruh2 = new UserDTO(buruhUserId, "buruh2", "Buruh 2", "BURUH", "buruh2@example.com", null, null);

        when(userRepository.findById(buruhId)).thenReturn(buruh);
        when(userRepository.findById(buruhUserId)).thenReturn(buruh2);

        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.assignBuruhToMandor(buruhId, buruhUserId)
        );
    }

    // ===================== UNASSIGN TESTS =====================

    @Test
    @DisplayName("Should unassign buruh from mandor successfully")
    void testUnassignBuruhFromMandorSuccess() {
        UUID buruhId = UUID.randomUUID();
        UserDTO buruh = new UserDTO(buruhId, "buruh", "Buruh User", "BURUH", "buruh@example.com", null, mandorId);

        when(userRepository.findById(buruhId)).thenReturn(buruh);

        authCommandUseCase.unassignBuruhFromMandor(buruhId);

        verify(userRepository, times(1)).removeBuruhMandorAssignment(buruhId);
    }

    @Test
    @DisplayName("Should throw exception when unassigning non-BURUH user")
    void testUnassignNonBuruhUser() {
        UUID mandorUserId = UUID.randomUUID();
        UserDTO mandor = new UserDTO(mandorUserId, "mandor", "Mandor User", "MANDOR", "mandor@example.com", "CERT-001", null);

        when(userRepository.findById(mandorUserId)).thenReturn(mandor);

        assertThrows(IllegalArgumentException.class, () ->
                authCommandUseCase.unassignBuruhFromMandor(mandorUserId)
        );
    }

    // ===================== LOGOUT TESTS =====================

    @Test
    @DisplayName("Should logout user")
    void testLogout() {
        authCommandUseCase.logout(userId);
        // Logout is currently a no-op, so we just verify it doesn't throw
        assertTrue(true);
    }
}
