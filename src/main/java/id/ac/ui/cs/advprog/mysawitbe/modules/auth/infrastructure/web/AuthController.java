package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.*;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthCommandUseCase authCommandUseCase;

    public AuthController(AuthCommandUseCase authCommandUseCase) {
        this.authCommandUseCase = authCommandUseCase;
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
                request.email(), request.password(), request.name(), request.role().name());
        return ResponseEntity.status(201).body(ApiResponse.success(user));
    }

    /** GET /api/auth/oauth2/url — returns Google OAuth2 authorization URL */
    @GetMapping("/oauth2/url")
    public ResponseEntity<ApiResponse<GoogleOAuthUrlDTO>> getOAuthUrl() {
        GoogleOAuthUrlDTO dto = authCommandUseCase.getGoogleOAuthUrl();
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /** GET /api/auth/oauth2/callback — Google redirects here */
    @GetMapping("/oauth2/callback")
    public void oauthCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {
        GoogleOAuthCallbackDTO callback = new GoogleOAuthCallbackDTO(code, state);
        AuthTokenDTO token = authCommandUseCase.handleGoogleOAuthCallback(
                callback.code(), callback.state());

        String frontendUrl = "http://localhost:3000/auth/callback"
                + "?token=" + token.accessToken()
                + "&role=" + token.role();

        response.sendRedirect(frontendUrl);
    }

    /** POST /api/auth/logout */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestAttribute("userId") java.util.UUID userId) {
        authCommandUseCase.logout(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}