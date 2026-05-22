package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.*;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthCommandUseCase authCommandUseCase;
    private final String frontendUrl;

    public AuthController(AuthCommandUseCase authCommandUseCase,
                          @org.springframework.beans.factory.annotation.Value("${app.frontend.url}") String frontendUrl) {
        this.authCommandUseCase = authCommandUseCase;
        this.frontendUrl = frontendUrl;
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        AuthTokenDTO token = authCommandUseCase.loginWithEmail(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.success(token));
    }

    /** POST /api/auth/register */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        UserDTO user = authCommandUseCase.registerUser(
            request.email(), request.password(), request.name(), request.role().name(), request.mandorCertificationNumber());
        return ResponseEntity.status(201).body(ApiResponse.success(user));
    }

    /** GET /api/auth/oauth2/url - returns Google OAuth2 authorization URL */
    @GetMapping("/oauth2/url")
    public ResponseEntity<ApiResponse<GoogleOAuthUrlDTO>> getOAuthUrl() {
        GoogleOAuthUrlDTO dto = authCommandUseCase.getGoogleOAuthUrl();
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /** GET /api/auth/oauth2/callback - Google redirects here */
    @GetMapping("/oauth2/callback")
    public void oauthCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {
        GoogleOAuthCallbackDTO callback = new GoogleOAuthCallbackDTO(code, state);
        OAuthCallbackResultDTO result = authCommandUseCase.handleGoogleOAuthCallback(
                callback.code(), callback.state());

        String redirectUrl;
        if (result.registrationRequired()) {
            redirectUrl = frontendUrl + "/auth/callback"
                + "?registrationToken=" + urlEncode(result.registrationToken())
                + "&email=" + urlEncode(result.email())
                + "&name=" + urlEncode(result.name());
        } else {
            redirectUrl = frontendUrl + "/auth/callback"
                + "?token=" + urlEncode(result.accessToken())
                + "&role=" + urlEncode(result.role());
        }

        response.sendRedirect(redirectUrl);
    }

        /** POST /api/auth/oauth2/complete-registration */
        @PostMapping("/oauth2/complete-registration")
        public ResponseEntity<ApiResponse<AuthTokenDTO>> completeOauthRegistration(
            @Valid @RequestBody OAuthCompleteRegistrationRequestDTO request) {
        AuthTokenDTO token = authCommandUseCase.completeGoogleOAuthRegistration(
            request.registrationToken(),
            request.role().name(),
            request.mandorCertificationNumber());
        return ResponseEntity.ok(ApiResponse.success(token));
        }

    /** POST /api/auth/logout */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestAttribute("userId") java.util.UUID userId) {
        authCommandUseCase.logout(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}