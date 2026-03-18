package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.*;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock AuthCommandUseCase  authCommandUseCase;
    @Mock HttpServletResponse httpResponse;

    AuthController controller;

    final String frontendUrl = "http://localhost:3000";
    final UUID   userId      = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new AuthController(authCommandUseCase, frontendUrl);
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("valid credentials – 200 with token body")
        void validCredentialsReturns200() {
            LoginRequestDTO req   = new LoginRequestDTO("u@t.com", "pass");
            AuthTokenDTO    token = new AuthTokenDTO("jwt", "BURUH");
            when(authCommandUseCase.loginWithEmail("u@t.com", "pass")).thenReturn(token);

            ResponseEntity<?> resp = controller.login(req);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
        }

        @Test
        @DisplayName("service throws – exception propagates from controller")
        void serviceExceptionPropagates() {
            LoginRequestDTO req = new LoginRequestDTO("u@t.com", "wrong");
            when(authCommandUseCase.loginWithEmail(any(), any()))
                    .thenThrow(new IllegalArgumentException("Password salah"));

            assertThatThrownBy(() -> controller.login(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("valid request – 201 with created user body")
        void validRequestReturns201() {
            UserDTO user = new UserDTO(userId, "u", "Name", "BURUH", "u@t.com");
            RegisterRequestDTO req = new RegisterRequestDTO(
                    "u@t.com", "pass", "Name", UserRole.BURUH);
            when(authCommandUseCase.registerUser("u@t.com", "pass", "Name", "BURUH"))
                    .thenReturn(user);

            ResponseEntity<?> resp = controller.register(req);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("duplicate email – service exception propagates")
        void duplicateEmailPropagates() {
            RegisterRequestDTO req = new RegisterRequestDTO(
                    "u@t.com", "pass", "Name", UserRole.BURUH);
            when(authCommandUseCase.registerUser(any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("already registered"));

            assertThatThrownBy(() -> controller.register(req))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("getOAuthUrl")
    class GetOAuthUrl {

        @Test
        @DisplayName("returns 200 with authorization URL and state")
        void returns200WithUrlAndState() {
            GoogleOAuthUrlDTO dto = new GoogleOAuthUrlDTO("https://accounts.google.com/?...", "state");
            when(authCommandUseCase.getGoogleOAuthUrl()).thenReturn(dto);

            ResponseEntity<?> resp = controller.getOAuthUrl();

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
        }
    }

    @Nested
    @DisplayName("oauthCallback")
    class OauthCallback {

        @Test
        @DisplayName("valid callback – redirects to frontend with token and role params")
        void validCallbackRedirectsToFrontend() throws IOException {
            AuthTokenDTO token = new AuthTokenDTO("jwt.tok", "BURUH");
            when(authCommandUseCase.handleGoogleOAuthCallback("code123", "state123"))
                    .thenReturn(token);

            controller.oauthCallback("code123", "state123", httpResponse);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpResponse).sendRedirect(urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl)
                    .startsWith(frontendUrl + "/auth/callback")
                    .contains("token=jwt.tok")
                    .contains("role=BURUH");
        }

        @Test
        @DisplayName("invalid state – service exception propagates")
        void invalidStateExceptionPropagates() {
            when(authCommandUseCase.handleGoogleOAuthCallback(any(), any()))
                    .thenThrow(new IllegalArgumentException("Invalid or expired OAuth state"));

            assertThatThrownBy(() ->
                    controller.oauthCallback("code", "bad-state", httpResponse))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("state");
        }
    }

    @Test
    @DisplayName("logout – 200 returned and use case called with userId")
    void logoutReturns200AndCallsUseCase() {
        ResponseEntity<?> resp = controller.logout(userId);

        verify(authCommandUseCase).logout(userId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}