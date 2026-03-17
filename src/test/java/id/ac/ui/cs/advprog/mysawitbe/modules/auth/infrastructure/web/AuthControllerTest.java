package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock AuthCommandUseCase  authCommandUseCase;
    @Mock HttpServletResponse httpResponse;

    AuthController controller;

    @BeforeEach
    void setUp() {
        // TODO: instantiate controller with mocked dependencies
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("valid credentials 200 with token body")
        void validCredentialsReturns200() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("service throws exception propagates from controller")
        void serviceExceptionPropagates() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("valid request 201 with created user body")
        void validRequestReturns201() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("duplicate email service exception propagates")
        void duplicateEmailPropagates() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("getOAuthUrl")
    class GetOAuthUrl {

        @Test
        @DisplayName("returns 200 with authorization URL and state")
        void returns200WithUrlAndState() {
            fail("not yet implemented");
        }
    }

    @Nested
    @DisplayName("oauthCallback")
    class OauthCallback {

        @Test
        @DisplayName("valid callback redirects to frontend with token and role params")
        void validCallbackRedirectsToFrontend() {
            fail("not yet implemented");
        }

        @Test
        @DisplayName("invalid state service exception propagates")
        void invalidStateExceptionPropagates() {
            fail("not yet implemented");
        }
    }

    @Test
    @DisplayName("logout 200 returned and use case called with userId")
    void logoutReturns200AndCallsUseCase() {
        fail("not yet implemented");
    }
}