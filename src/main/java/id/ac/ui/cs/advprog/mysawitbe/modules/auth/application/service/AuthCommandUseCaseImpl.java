package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.*;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.event.BuruhAssignedEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.OAuth2Port;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.UserRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuthCommandUseCaseImpl implements AuthCommandUseCase {

    private static final Duration OAUTH_STATE_TTL = Duration.ofMinutes(10);

    private final String googleRedirectUri;

    private final UserRepositoryPort  userRepository;
    private final OAuth2Port          oauth2Port;
    private final JwtService          jwtService;
    private final PasswordEncoder     passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    private final String googleClientId;

    public AuthCommandUseCaseImpl(UserRepositoryPort userRepository,
                                  OAuth2Port oauth2Port,
                                  JwtService jwtService,
                                  PasswordEncoder passwordEncoder,
                                  ApplicationEventPublisher eventPublisher,
                                  @Value("${GOOGLE_CLIENT_ID:placeholder-client-id}") String googleClientId,
                                  @Value("${app.google.redirect-uri}") String googleRedirectUri) {
        this.userRepository   = userRepository;
        this.oauth2Port       = oauth2Port;
        this.jwtService       = jwtService;
        this.passwordEncoder  = passwordEncoder;
        this.eventPublisher   = eventPublisher;
        this.googleClientId   = googleClientId;
        this.googleRedirectUri = googleRedirectUri;
    }

    @Override
    public AuthTokenDTO loginWithEmail(String email, String password) {
        String hash = userRepository.findPasswordHashByEmail(email);

        if (hash == null) {
            UserDTO user = userRepository.findByEmail(email);
            if (user != null) {
                throw new IllegalArgumentException(
                        "Akun ini terdaftar via Google. Silakan login dengan Google."
                );
            }
            throw new IllegalArgumentException("Email tidak terdaftar");
        }

        if (!passwordEncoder.matches(password, hash)) {
            throw new IllegalArgumentException("Password salah");
        }

        UserDTO user = userRepository.findByEmail(email);
        String token = jwtService.generateToken(user.userId().toString(), user.role());
        return new AuthTokenDTO(token, user.role());
    }

    @Override
    public UserDTO registerUser(String email, String password, String name, String role, String mandorCertificationNumber) {
        String normalizedRole = normalizeRole(role);
        validateRoleNotAdmin(normalizedRole);
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalStateException("Email already registered");
        }
        String normalizedCertificationNumber = normalizeMandorCertificationNumber(mandorCertificationNumber);
        validateMandorCertificationForRegistration(normalizedRole, normalizedCertificationNumber);

        // username derived from email local-part
        String username = email.split("@")[0];
        String hashed   = passwordEncoder.encode(password);
        UserDTO dto = new UserDTO(
                null,
                username,
                name,
                normalizedRole,
                email,
                UserRole.MANDOR.name().equals(normalizedRole) ? normalizedCertificationNumber : null,
                null
        );
        return userRepository.save(dto, hashed);
    }

    @Override
    public GoogleOAuthUrlDTO getGoogleOAuthUrl() {
        String state = oauth2Port.generateState();
        oauth2Port.storeState(state, OAUTH_STATE_TTL);
        String url = buildGoogleAuthUrl(state);
        return new GoogleOAuthUrlDTO(url, state);
    }

    @Override
    public OAuthCallbackResultDTO handleGoogleOAuthCallback(String code, String state) {
        if (!oauth2Port.validateAndConsumeState(state)) {
            throw new IllegalArgumentException("Invalid or expired OAuth state");
        }
        Map<String, Object> tokens = oauth2Port.exchangeCodeForTokens(code, googleRedirectUri);
        String email = (String) tokens.get("email");
        String name  = (String) tokens.get("name");

        if (email == null || email.isBlank() || name == null || name.isBlank()) {
            throw new IllegalStateException("Incomplete user info from Google OAuth");
        }

        UserDTO user = userRepository.findByEmail(email);
        if (user != null) {
            String token = jwtService.generateToken(user.userId().toString(), user.role());
            return OAuthCallbackResultDTO.authenticated(token, user.role());
        }

        String registrationToken = jwtService.generateOAuthRegistrationToken(email, name);
        return OAuthCallbackResultDTO.registrationRequired(registrationToken, email, name);
    }

    @Override
    public AuthTokenDTO completeGoogleOAuthRegistration(String registrationToken, String role, String mandorCertificationNumber) {
        String normalizedRole = normalizeRole(role);
        validateRoleNotAdmin(normalizedRole);
        String normalizedCertificationNumber = normalizeMandorCertificationNumber(mandorCertificationNumber);
        validateMandorCertificationForRegistration(normalizedRole, normalizedCertificationNumber);

        OAuthPendingRegistrationDTO pendingRegistration;
        try {
            pendingRegistration = jwtService.extractOAuthPendingRegistration(registrationToken);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid or expired registration token");
        }

        UserDTO existing = userRepository.findByEmail(pendingRegistration.email());
        if (existing != null) {
            String token = jwtService.generateToken(existing.userId().toString(), existing.role());
            return new AuthTokenDTO(token, existing.role());
        }

        String username = pendingRegistration.email().split("@")[0];
        UserDTO newUser = new UserDTO(
                null,
                username,
                pendingRegistration.name(),
                normalizedRole,
                pendingRegistration.email(),
                UserRole.MANDOR.name().equals(normalizedRole) ? normalizedCertificationNumber : null,
                null
        );

        UserDTO saved = userRepository.save(newUser, null);
        String token = jwtService.generateToken(saved.userId().toString(), saved.role());
        return new AuthTokenDTO(token, saved.role());
    }

    @Override
    public void logout(UUID userId) {

    }

    @Override
    public UserDTO editUser(UUID userId, String name, String role, String email, String mandorCertificationNumber) {
        UserDTO existing = getExistingUser(userId);
        if (UserRole.ADMIN.name().equals(existing.role())) {
            throw new IllegalStateException("Cannot edit another admin account");
        }
        String normalizedRole = normalizeRole(role);
        String normalizedCertificationNumber = normalizeMandorCertificationNumber(mandorCertificationNumber);
        String effectiveCertificationNumber;

        if (UserRole.MANDOR.name().equals(normalizedRole)) {
            effectiveCertificationNumber = normalizedCertificationNumber != null
                    ? normalizedCertificationNumber
                    : normalizeMandorCertificationNumber(existing.mandorCertificationNumber());
        } else {
            effectiveCertificationNumber = null;
        }

        validateMandorCertificationForRegistration(normalizedRole, effectiveCertificationNumber);

        UserDTO updated = new UserDTO(
                existing.userId(),
                existing.username(),
                name,
                normalizedRole,
                email,
                effectiveCertificationNumber,
                existing.mandorId()
        );
        return userRepository.save(updated, userRepository.findPasswordHashByEmail(existing.email()));
    }

    @Override
    public void deleteUser(UUID requestingAdminId, UUID targetUserId) {
        if (requestingAdminId.equals(targetUserId)) {
            throw new IllegalArgumentException("Admin cannot delete their own account");
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new EntityNotFoundException("User not found: " + targetUserId);
        }
        userRepository.deleteById(targetUserId);
    }

    @Override
    public void assignBuruhToMandor(UUID buruhId, UUID mandorId) {
        UserDTO buruh  = getExistingUser(buruhId);
        UserDTO mandor = getExistingUser(mandorId);

        if (!UserRole.BURUH.name().equals(buruh.role())) {
            throw new IllegalArgumentException("Target user is not a BURUH");
        }
        if (!UserRole.MANDOR.name().equals(mandor.role())) {
            throw new IllegalArgumentException("Target user is not a MANDOR");
        }
        userRepository.saveBuruhMandorAssignment(buruhId, mandorId);
        eventPublisher.publishEvent(new BuruhAssignedEvent(buruhId, mandorId));
    }

    @Override
    public void unassignBuruhFromMandor(UUID buruhId) {
        UserDTO buruh = getExistingUser(buruhId);
        if (!UserRole.BURUH.name().equals(buruh.role())) {
            throw new IllegalArgumentException("Target user is not a BURUH");
        }
        userRepository.removeBuruhMandorAssignment(buruhId);
    }

    // Helper method
    private UserDTO getExistingUser(UUID userId) {
        UserDTO user = userRepository.findById(userId);
        if (user == null) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
        return user;
    }

    private void validateRoleNotAdmin(String role) {
        if (UserRole.ADMIN.name().equals(role)) {
            throw new IllegalArgumentException("Cannot self-register as ADMIN");
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeMandorCertificationNumber(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateMandorCertificationForRegistration(String role, String mandorCertificationNumber) {
        if (UserRole.MANDOR.name().equals(role) && mandorCertificationNumber == null) {
            throw new IllegalArgumentException("Nomor sertifikasi mandor wajib diisi");
        }
    }

    private String buildGoogleAuthUrl(String state) {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
               "?client_id=" + googleClientId +
               "&redirect_uri=" + googleRedirectUri +
               "&response_type=code" +
               "&scope=openid%20email%20profile" +
               "&state=" + state;
    }
}
