package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.*;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.event.BuruhAssignedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.OAuth2Port;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.UserRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthCommandUseCaseImpl")
class AuthCommandUseCaseImplTest {

    @Mock UserRepositoryPort     userRepository;
    @Mock OAuth2Port             oauth2Port;
    @Mock JwtService             jwtService;
    @Mock PasswordEncoder        passwordEncoder;
    @Mock ApplicationEventPublisher eventPublisher;

    AuthCommandUseCaseImpl service;

    final UUID   userId   = UUID.randomUUID();
    final UUID   mandorId = UUID.randomUUID();
    final String email    = "user@test.com";
    final String password = "secret";
    final String hash     = "$2a$hash";
    final String token    = "jwt.token.here";

    @BeforeEach
    void setUp() {
        service = new AuthCommandUseCaseImpl(
                userRepository, oauth2Port, jwtService,
                passwordEncoder, eventPublisher,
                "google-client-id",
                "http://localhost:8080/api/auth/oauth2/callback"
        );
    }

    @Nested
    @DisplayName("loginWithEmail")
    class LoginWithEmail {

        @Test
        @DisplayName("valid credentials – returns JWT token with role and Bearer type")
        void validCredentialsReturnToken() {
            UserDTO user = new UserDTO(userId, "user", "User Name", "BURUH", email);
            when(userRepository.findPasswordHashByEmail(email)).thenReturn(hash);
            when(passwordEncoder.matches(password, hash)).thenReturn(true);
            when(userRepository.findByEmail(email)).thenReturn(user);
            when(jwtService.generateToken(userId.toString(), "BURUH")).thenReturn(token);

            AuthTokenDTO result = service.loginWithEmail(email, password);

            assertThat(result.accessToken()).isEqualTo(token);
            assertThat(result.role()).isEqualTo("BURUH");
            assertThat(result.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("email not registered – throws IllegalArgumentException")
        void emailNotRegisteredThrows() {
            when(userRepository.findPasswordHashByEmail(email)).thenReturn(null);
            when(userRepository.findByEmail(email)).thenReturn(null);

            assertThatThrownBy(() -> service.loginWithEmail(email, password))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tidak terdaftar");
        }

        @Test
        @DisplayName("OAuth-only account – throws with Google-specific message")
        void oauthOnlyAccountThrowsGoogleMessage() {
            UserDTO oauthUser = new UserDTO(userId, "user", "Name", "BURUH", email);
            when(userRepository.findPasswordHashByEmail(email)).thenReturn(null);
            when(userRepository.findByEmail(email)).thenReturn(oauthUser);

            assertThatThrownBy(() -> service.loginWithEmail(email, password))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Google");
        }

        @Test
        @DisplayName("wrong password – throws IllegalArgumentException")
        void wrongPasswordThrows() {
            when(userRepository.findPasswordHashByEmail(email)).thenReturn(hash);
            when(passwordEncoder.matches(password, hash)).thenReturn(false);

            assertThatThrownBy(() -> service.loginWithEmail(email, password))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password salah");
        }
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("new user – saved and returned correctly")
        void newUserSavedAndReturned() {
            UserDTO saved = new UserDTO(userId, "user", "User Name", "BURUH", email);
            when(userRepository.findByEmail(email)).thenReturn(null);
            when(passwordEncoder.encode(password)).thenReturn(hash);
            when(userRepository.save(any(), eq(hash))).thenReturn(saved);

            UserDTO result = service.registerUser(email, password, "User Name", "BURUH");

            assertThat(result.email()).isEqualTo(email);
            assertThat(result.role()).isEqualTo("BURUH");
            verify(userRepository).save(any(UserDTO.class), eq(hash));
        }

        @Test
        @DisplayName("duplicate email – throws IllegalStateException")
        void duplicateEmailThrows() {
            when(userRepository.findByEmail(email))
                    .thenReturn(new UserDTO(userId, "user", "Name", "BURUH", email));

            assertThatThrownBy(() -> service.registerUser(email, password, "Name", "BURUH"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("self-register as ADMIN – throws IllegalArgumentException")
        void selfRegisterAsAdminThrows() {
            assertThatThrownBy(() -> service.registerUser(email, password, "Name", "ADMIN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ADMIN");
        }

        @Test
        @DisplayName("username derived from email local-part")
        void usernameDerivedFromEmailLocalPart() {
            ArgumentCaptor<UserDTO> captor = ArgumentCaptor.forClass(UserDTO.class);
            UserDTO saved = new UserDTO(userId, "user", "Name", "MANDOR", email);
            when(userRepository.findByEmail(email)).thenReturn(null);
            when(passwordEncoder.encode(password)).thenReturn(hash);
            when(userRepository.save(captor.capture(), eq(hash))).thenReturn(saved);

            service.registerUser(email, password, "Name", "MANDOR");

            assertThat(captor.getValue().username()).isEqualTo("user");
        }
    }

    @Nested
    @DisplayName("getGoogleOAuthUrl")
    class GetGoogleOAuthUrl {

        @Test
        @DisplayName("returns URL containing state and client_id")
        void returnsUrlWithStateAndClientId() {
            when(oauth2Port.generateState()).thenReturn("random-state");
            when(oauth2Port.storeState(eq("random-state"), any())).thenReturn(true);

            GoogleOAuthUrlDTO result = service.getGoogleOAuthUrl();

            assertThat(result.state()).isEqualTo("random-state");
            assertThat(result.authorizationUrl())
                    .contains("google-client-id")
                    .contains("random-state")
                    .contains("accounts.google.com");
        }

        @Test
        @DisplayName("storeState called with exactly 10-minute TTL")
        void storeStateCalledWithTenMinuteTtl() {
            when(oauth2Port.generateState()).thenReturn("s");
            when(oauth2Port.storeState(any(), any())).thenReturn(true);

            service.getGoogleOAuthUrl();

            verify(oauth2Port).storeState(eq("s"),
                    argThat(d -> d.toMinutes() == 10));
        }
    }

    @Nested
    @DisplayName("handleGoogleOAuthCallback")
    class HandleGoogleOAuthCallback {

        final String code  = "auth-code";
        final String state = "valid-state";

        @Test
        @DisplayName("existing user – returns token without creating new user")
        void existingUserReturnsToken() {
            UserDTO user = new UserDTO(userId, "guser", "G User", "BURUH", email);
            when(oauth2Port.validateAndConsumeState(state)).thenReturn(true);
            when(oauth2Port.exchangeCodeForTokens(eq(code), any()))
                    .thenReturn(Map.of("email", email, "name", "G User"));
            when(userRepository.findByEmail(email)).thenReturn(user);
            when(jwtService.generateToken(userId.toString(), "BURUH")).thenReturn(token);

            AuthTokenDTO result = service.handleGoogleOAuthCallback(code, state);

            assertThat(result.accessToken()).isEqualTo(token);
            verify(userRepository, never()).save(any(), any());
        }

        @Test
        @DisplayName("new user – auto-registered as BURUH")
        void newUserAutoRegisteredAsBuruh() {
            UserDTO created = new UserDTO(userId, "guser", "G User", "BURUH", email);
            when(oauth2Port.validateAndConsumeState(state)).thenReturn(true);
            when(oauth2Port.exchangeCodeForTokens(eq(code), any()))
                    .thenReturn(Map.of("email", email, "name", "G User"));
            when(userRepository.findByEmail(email)).thenReturn(null);
            when(userRepository.save(any(), isNull())).thenReturn(created);
            when(jwtService.generateToken(userId.toString(), "BURUH")).thenReturn(token);

            AuthTokenDTO result = service.handleGoogleOAuthCallback(code, state);

            assertThat(result.accessToken()).isEqualTo(token);
            verify(userRepository).save(argThat(u -> "BURUH".equals(u.role())), isNull());
        }

        @Test
        @DisplayName("invalid state – throws IllegalArgumentException")
        void invalidStateThrows() {
            when(oauth2Port.validateAndConsumeState(state)).thenReturn(false);

            assertThatThrownBy(() -> service.handleGoogleOAuthCallback(code, state))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("state");
        }
    }

    @Nested
    @DisplayName("editUser")
    class EditUser {

        @Test
        @DisplayName("non-admin user – updated and returned")
        void nonAdminUserUpdated() {
            UserDTO existing = new UserDTO(userId, "u", "Old", "BURUH", email);
            UserDTO updated  = new UserDTO(userId, "u", "New", "MANDOR", email);
            when(userRepository.findById(userId)).thenReturn(existing);
            when(userRepository.findPasswordHashByEmail(email)).thenReturn(hash);
            when(userRepository.save(any(), eq(hash))).thenReturn(updated);

            UserDTO result = service.editUser(userId, "New", "MANDOR", email);

            assertThat(result.name()).isEqualTo("New");
            assertThat(result.role()).isEqualTo("MANDOR");
        }

        @Test
        @DisplayName("editing ADMIN account – throws IllegalStateException")
        void editingAdminThrows() {
            when(userRepository.findById(userId))
                    .thenReturn(new UserDTO(userId, "a", "Admin", "ADMIN", email));

            assertThatThrownBy(() -> service.editUser(userId, "New", "ADMIN", email))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("admin");
        }

        @Test
        @DisplayName("user not found – throws EntityNotFoundException")
        void userNotFoundThrows() {
            when(userRepository.findById(userId)).thenReturn(null);

            assertThatThrownBy(() -> service.editUser(userId, "N", "BURUH", email))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        final UUID adminId  = UUID.randomUUID();
        final UUID targetId = UUID.randomUUID();

        @Test
        @DisplayName("valid delete – repository deleteById called")
        void validDeleteCallsRepository() {
            when(userRepository.existsById(targetId)).thenReturn(true);

            service.deleteUser(adminId, targetId);

            verify(userRepository).deleteById(targetId);
        }

        @Test
        @DisplayName("admin deletes self – throws IllegalArgumentException")
        void selfDeleteThrows() {
            assertThatThrownBy(() -> service.deleteUser(adminId, adminId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("own account");
        }

        @Test
        @DisplayName("target not found – throws EntityNotFoundException")
        void targetNotFoundThrows() {
            when(userRepository.existsById(targetId)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteUser(adminId, targetId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("assignBuruhToMandor")
    class AssignBuruhToMandor {

        final UUID buruhId = UUID.randomUUID();

        @Test
        @DisplayName("valid assignment – saved and BuruhAssignedEvent published")
        void validAssignmentSavesAndPublishesEvent() {
            UserDTO buruh  = new UserDTO(buruhId,  "b", "Buruh",  "BURUH",  "b@t.com");
            UserDTO mandor = new UserDTO(mandorId, "m", "Mandor", "MANDOR", "m@t.com");
            when(userRepository.findById(buruhId)).thenReturn(buruh);
            when(userRepository.findById(mandorId)).thenReturn(mandor);

            service.assignBuruhToMandor(buruhId, mandorId);

            verify(userRepository).saveBuruhMandorAssignment(buruhId, mandorId);
            verify(eventPublisher).publishEvent(new BuruhAssignedEvent(buruhId, mandorId));
        }

        @Test
        @DisplayName("target is not BURUH – throws IllegalArgumentException")
        void targetNotBuruhThrows() {
            when(userRepository.findById(buruhId))
                    .thenReturn(new UserDTO(buruhId, "m", "Mandor", "MANDOR", "m@t.com"));
            when(userRepository.findById(mandorId))
                    .thenReturn(new UserDTO(mandorId, "m2", "Mandor2", "MANDOR", "m2@t.com"));

            assertThatThrownBy(() -> service.assignBuruhToMandor(buruhId, mandorId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BURUH");
        }

        @Test
        @DisplayName("target is not MANDOR – throws IllegalArgumentException")
        void targetNotMandorThrows() {
            when(userRepository.findById(buruhId))
                    .thenReturn(new UserDTO(buruhId, "b", "Buruh", "BURUH", "b@t.com"));
            when(userRepository.findById(mandorId))
                    .thenReturn(new UserDTO(mandorId, "b2", "Buruh2", "BURUH", "b2@t.com"));

            assertThatThrownBy(() -> service.assignBuruhToMandor(buruhId, mandorId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MANDOR");
        }

        @Test
        @DisplayName("buruh not found – throws EntityNotFoundException")
        void buruhNotFoundThrows() {
            when(userRepository.findById(buruhId)).thenReturn(null);

            assertThatThrownBy(() -> service.assignBuruhToMandor(buruhId, mandorId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("unassignBuruhFromMandor")
    class UnassignBuruh {

        final UUID buruhId = UUID.randomUUID();

        @Test
        @DisplayName("valid unassign – removeBuruhMandorAssignment called")
        void validUnassignCallsRepository() {
            when(userRepository.findById(buruhId))
                    .thenReturn(new UserDTO(buruhId, "b", "Buruh", "BURUH", "b@t.com"));

            service.unassignBuruhFromMandor(buruhId);

            verify(userRepository).removeBuruhMandorAssignment(buruhId);
        }

        @Test
        @DisplayName("target is not BURUH – throws IllegalArgumentException")
        void targetNotBuruhThrows() {
            when(userRepository.findById(buruhId))
                    .thenReturn(new UserDTO(buruhId, "m", "Mandor", "MANDOR", "m@t.com"));

            assertThatThrownBy(() -> service.unassignBuruhFromMandor(buruhId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BURUH");
        }

        @Test
        @DisplayName("user not found – throws EntityNotFoundException")
        void userNotFoundThrows() {
            when(userRepository.findById(buruhId)).thenReturn(null);

            assertThatThrownBy(() -> service.unassignBuruhFromMandor(buruhId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Test
    @DisplayName("logout – completes without exception (no-op)")
    void logoutCompletesWithoutException() {
        assertThatCode(() -> service.logout(userId)).doesNotThrowAnyException();
    }
}