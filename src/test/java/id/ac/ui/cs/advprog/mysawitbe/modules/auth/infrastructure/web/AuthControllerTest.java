package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.*;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthCommandUseCase authCommandUseCase;

    @Mock
    private HttpServletResponse httpServletResponse;

    private AuthController authController;
    private UUID userId;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authCommandUseCase, "http://localhost:3000");
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should login user successfully")
    void testLoginSuccess() {
        String email = "test@example.com";
        String password = "password123";
        String token = "jwt_token_123";
        LoginRequestDTO request = new LoginRequestDTO(email, password);
        AuthTokenDTO response = new AuthTokenDTO(token, "BURUH");

        when(authCommandUseCase.loginWithEmail(email, password)).thenReturn(response);

        ResponseEntity<ApiResponse<AuthTokenDTO>> result = authController.login(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().success());
        assertEquals(token, result.getBody().data().accessToken());
        verify(authCommandUseCase, times(1)).loginWithEmail(email, password);
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegisterSuccess() {
        String email = "newuser@example.com";
        String password = "password123";
        String name = "New User";
        UserRole role = UserRole.BURUH;
        RegisterRequestDTO request = new RegisterRequestDTO(email, password, name, role, null);
        UserDTO response = new UserDTO(userId, "newuser", name, "BURUH", email, null, null);

        when(authCommandUseCase.registerUser(email, password, name, role.name(), null)).thenReturn(response);

        ResponseEntity<ApiResponse<UserDTO>> result = authController.register(request);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().success());
        assertEquals(name, result.getBody().data().name());
        verify(authCommandUseCase, times(1)).registerUser(email, password, name, role.name(), null);
    }

    @Test
    @DisplayName("Should get Google OAuth URL")
    void testGetGoogleOAuthUrl() {
        String url = "https://accounts.google.com/o/oauth2/v2/auth?client_id=test&scope=openid&redirect_uri=http://localhost:8080/callback";
        String state = "random-state-123";
        GoogleOAuthUrlDTO response = new GoogleOAuthUrlDTO(url, state);

        when(authCommandUseCase.getGoogleOAuthUrl()).thenReturn(response);

        ResponseEntity<ApiResponse<GoogleOAuthUrlDTO>> result = authController.getOAuthUrl();

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().success());
        assertEquals(url, result.getBody().data().authorizationUrl());
        assertEquals(state, result.getBody().data().state());
        verify(authCommandUseCase, times(1)).getGoogleOAuthUrl();
    }

    @Test
    @DisplayName("Should redirect to complete registration when registration required")
    void testOAuthCallbackRegistrationRequired() throws IOException {
        String code = "google_auth_code";
        String state = "random_state";
        String registrationToken = "reg_token_xyz";
        String email = "newuser@example.com";
        String name = "New User";

        OAuthCallbackResultDTO result = OAuthCallbackResultDTO.registrationRequired(registrationToken, email, name);

        when(authCommandUseCase.handleGoogleOAuthCallback(code, state)).thenReturn(result);

        authController.oauthCallback(code, state, httpServletResponse);

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).sendRedirect(redirectCaptor.capture());
        String redirectUrl = redirectCaptor.getValue();
        assertTrue(redirectUrl.contains("registrationToken=" + registrationToken));
        assertTrue(redirectUrl.contains("email="));
        assertTrue(redirectUrl.contains("name="));
    }

    @Test
    @DisplayName("Should redirect to dashboard when registration not required")
    void testOAuthCallbackNoRegistration() throws IOException {
        String code = "google_auth_code";
        String state = "random_state";
        String accessToken = "jwt_token_from_oauth";
        String role = "BURUH";

        OAuthCallbackResultDTO result = OAuthCallbackResultDTO.authenticated(accessToken, role);

        when(authCommandUseCase.handleGoogleOAuthCallback(code, state)).thenReturn(result);

        authController.oauthCallback(code, state, httpServletResponse);

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).sendRedirect(redirectCaptor.capture());
        String redirectUrl = redirectCaptor.getValue();
        assertTrue(redirectUrl.contains("token=" + accessToken));
        assertTrue(redirectUrl.contains("role=" + role));
    }

    @Test
    @DisplayName("Should complete OAuth registration")
    void testCompleteOAuthRegistration() {
        String registrationToken = "reg_token_123";
        UserRole role = UserRole.BURUH;
        String certNumber = null;
        OAuthCompleteRegistrationRequestDTO request = new OAuthCompleteRegistrationRequestDTO(registrationToken, role, certNumber);
        String token = "jwt_token_456";
        AuthTokenDTO response = new AuthTokenDTO(token, "BURUH");

        when(authCommandUseCase.completeGoogleOAuthRegistration(registrationToken, role.name(), certNumber)).thenReturn(response);

        ResponseEntity<ApiResponse<AuthTokenDTO>> result = authController.completeOauthRegistration(request);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().success());
        assertEquals(token, result.getBody().data().accessToken());
        verify(authCommandUseCase, times(1)).completeGoogleOAuthRegistration(registrationToken, role.name(), certNumber);
    }

    @Test
    @DisplayName("Should logout user")
    void testLogout() {
        UUID logoutUserId = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> result = authController.logout(logoutUserId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        verify(authCommandUseCase, times(1)).logout(logoutUserId);
    }

    @Test
    @DisplayName("Should handle login with invalid credentials")
    void testLoginInvalidCredentials() {
        String email = "test@example.com";
        String password = "wrongpassword";
        LoginRequestDTO request = new LoginRequestDTO(email, password);

        when(authCommandUseCase.loginWithEmail(email, password))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        assertThrows(IllegalArgumentException.class, () -> authController.login(request));
    }

    @Test
    @DisplayName("Should handle register with existing email")
    void testRegisterExistingEmail() {
        String email = "existing@example.com";
        UserRole role = UserRole.BURUH;
        RegisterRequestDTO request = new RegisterRequestDTO(email, "password", "User", role, null);

        when(authCommandUseCase.registerUser(email, "password", "User", role.name(), null))
                .thenThrow(new IllegalStateException("Email already registered"));

        assertThrows(IllegalStateException.class, () -> authController.register(request));
    }

    @Test
    @DisplayName("Should handle register as ADMIN")
    void testRegisterAsAdmin() {
        RegisterRequestDTO request = new RegisterRequestDTO("admin@example.com", "password", "Admin", UserRole.ADMIN, null);

        when(authCommandUseCase.registerUser("admin@example.com", "password", "Admin", UserRole.ADMIN.name(), null))
                .thenThrow(new IllegalArgumentException("Cannot register as ADMIN"));

        assertThrows(IllegalArgumentException.class, () -> authController.register(request));
    }

    @Test
    @DisplayName("Should complete OAuth registration with MANDOR role")
    void testCompleteOAuthRegistrationWithMandor() {
        String registrationToken = "reg_token_mandor";
        UserRole role = UserRole.MANDOR;
        String certNumber = "CERT-12345";
        OAuthCompleteRegistrationRequestDTO request = new OAuthCompleteRegistrationRequestDTO(registrationToken, role, certNumber);
        String token = "jwt_token_mandor";
        AuthTokenDTO response = new AuthTokenDTO(token, "MANDOR");

        when(authCommandUseCase.completeGoogleOAuthRegistration(registrationToken, role.name(), certNumber)).thenReturn(response);

        ResponseEntity<ApiResponse<AuthTokenDTO>> result = authController.completeOauthRegistration(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(token, result.getBody().data().accessToken());
    }

    @Test
    @DisplayName("Should handle invalid OAuth state")
    void testGoogleOAuthCallbackInvalidState() {
        String code = "google_code";
        String state = "invalid_state";

        when(authCommandUseCase.handleGoogleOAuthCallback(code, state))
                .thenThrow(new IllegalArgumentException("Invalid state"));

        assertThrows(IllegalArgumentException.class, () -> authController.oauthCallback(code, state, null));
    }

    @Test
    @DisplayName("Should handle login with null password")
    void testLoginNullPassword() {
        String email = "test@example.com";
        LoginRequestDTO request = new LoginRequestDTO(email, null);

        when(authCommandUseCase.loginWithEmail(email, null))
                .thenThrow(new IllegalArgumentException("Password cannot be null"));

        assertThrows(IllegalArgumentException.class, () -> authController.login(request));
    }

    @Test
    @DisplayName("Should handle register as SUPIR")
    void testRegisterAsSupir() {
        String email = "supir@example.com";
        UserRole role = UserRole.SUPIR;
        RegisterRequestDTO request = new RegisterRequestDTO(email, "password123", "Supir User", role, null);
        UserDTO response = new UserDTO(UUID.randomUUID(), "supir", "Supir User", "SUPIR", email, null, null);

        when(authCommandUseCase.registerUser(email, "password123", "Supir User", role.name(), null))
                .thenReturn(response);

        ResponseEntity<ApiResponse<UserDTO>> result = authController.register(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals("SUPIR", result.getBody().data().role());
    }

    @Test
    @DisplayName("Should handle OAuth callback with special characters in email")
    void testOAuthCallbackWithSpecialCharacters() throws IOException {
        String code = "code_special";
        String state = "state_special";
        String registrationToken = "token_special";
        String email = "user+test@example.com";
        String name = "User & Friend";

        OAuthCallbackResultDTO result = OAuthCallbackResultDTO.registrationRequired(registrationToken, email, name);

        when(authCommandUseCase.handleGoogleOAuthCallback(code, state)).thenReturn(result);

        authController.oauthCallback(code, state, httpServletResponse);

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).sendRedirect(redirectCaptor.capture());
        String redirectUrl = redirectCaptor.getValue();
        assertTrue(redirectUrl.contains("registrationToken="));
    }

    @Test
    @DisplayName("Should handle complete OAuth registration as MANDOR")
    void testCompleteOAuthRegistrationWithMandorAndCertification() {
        String registrationToken = "reg_token_mandor";
        UserRole role = UserRole.MANDOR;
        String certNumber = "CERT-99999";
        OAuthCompleteRegistrationRequestDTO request = new OAuthCompleteRegistrationRequestDTO(registrationToken, role, certNumber);
        String token = "jwt_token_mandor";
        AuthTokenDTO response = new AuthTokenDTO(token, "MANDOR");

        when(authCommandUseCase.completeGoogleOAuthRegistration(registrationToken, role.name(), certNumber)).thenReturn(response);

        ResponseEntity<ApiResponse<AuthTokenDTO>> result = authController.completeOauthRegistration(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals("MANDOR", result.getBody().data().role());
    }

    @Test
    @DisplayName("Should handle login with empty email")
    void testLoginWithEmptyEmail() {
        String email = "";
        String password = "password";
        LoginRequestDTO request = new LoginRequestDTO(email, password);

        when(authCommandUseCase.loginWithEmail(email, password))
                .thenThrow(new IllegalArgumentException("Email is required"));

        assertThrows(IllegalArgumentException.class, () -> authController.login(request));
    }

    @Test
    @DisplayName("Should handle register with empty name")
    void testRegisterWithEmptyName() {
        RegisterRequestDTO request = new RegisterRequestDTO("test@example.com", "password", "", UserRole.BURUH, null);

        when(authCommandUseCase.registerUser("test@example.com", "password", "", UserRole.BURUH.name(), null))
                .thenThrow(new IllegalArgumentException("Name is required"));

        assertThrows(IllegalArgumentException.class, () -> authController.register(request));
    }

    @Test
    @DisplayName("Should handle OAuth callback when user info is incomplete")
    void testOAuthCallbackWithIncompleteUserInfo() throws IOException {
        String code = "code_incomplete";
        String state = "state_incomplete";

        when(authCommandUseCase.handleGoogleOAuthCallback(code, state))
                .thenThrow(new IllegalStateException("Incomplete user info from Google"));

        assertThrows(IllegalStateException.class, () -> authController.oauthCallback(code, state, httpServletResponse));
    }

    @Test
    @DisplayName("Should handle complete OAuth registration with invalid token")
    void testCompleteOAuthRegistrationWithInvalidToken() {
        OAuthCompleteRegistrationRequestDTO request = new OAuthCompleteRegistrationRequestDTO("invalid_token", UserRole.BURUH, null);

        when(authCommandUseCase.completeGoogleOAuthRegistration("invalid_token", UserRole.BURUH.name(), null))
                .thenThrow(new IllegalArgumentException("Invalid registration token"));

        assertThrows(IllegalArgumentException.class, () -> authController.completeOauthRegistration(request));
    }

    @Test
    @DisplayName("Should handle login with valid credentials from different roles")
    void testLoginWithDifferentRoles() {
        String[] roles = {"BURUH", "MANDOR", "SUPIR"};

        for (String role : roles) {
            String email = "user-" + role.toLowerCase() + "@example.com";
            String password = "password123";
            String token = "token-" + role;
            LoginRequestDTO request = new LoginRequestDTO(email, password);
            AuthTokenDTO response = new AuthTokenDTO(token, role);

            when(authCommandUseCase.loginWithEmail(email, password)).thenReturn(response);

            ResponseEntity<ApiResponse<AuthTokenDTO>> result = authController.login(request);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(role, result.getBody().data().role());
        }
    }

    @Test
    @DisplayName("Should handle complete OAuth with SUPIR role")
    void testCompleteOAuthRegistrationWithSupir() {
        String registrationToken = "reg_token_supir";
        UserRole role = UserRole.SUPIR;
        OAuthCompleteRegistrationRequestDTO request = new OAuthCompleteRegistrationRequestDTO(registrationToken, role, null);
        String token = "jwt_token_supir";
        AuthTokenDTO response = new AuthTokenDTO(token, "SUPIR");

        when(authCommandUseCase.completeGoogleOAuthRegistration(registrationToken, role.name(), null))
                .thenReturn(response);

        ResponseEntity<ApiResponse<AuthTokenDTO>> result = authController.completeOauthRegistration(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("SUPIR", result.getBody().data().role());
    }
}
