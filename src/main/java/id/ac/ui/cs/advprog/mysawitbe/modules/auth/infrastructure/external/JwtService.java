package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.OAuthPendingRegistrationDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private static final long OAUTH_REGISTRATION_EXPIRATION_MS = 10 * 60 * 1000;
    private static final String OAUTH_REGISTRATION_TYPE = "oauth-registration";

    private static final long UPLOAD_EXPIRATION_MS = 5 * 60 * 1000;
    private static final String UPLOAD_TYPE = "upload-token";

    private final SecretKey key;
    private final long      expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key          = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    public String generateOAuthRegistrationToken(String email, String name) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(OAUTH_REGISTRATION_TYPE)
                .claim("type", OAUTH_REGISTRATION_TYPE)
                .claim("email", email)
                .claim("name", name)
                .issuedAt(new Date(now))
                .expiration(new Date(now + OAUTH_REGISTRATION_EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    public OAuthPendingRegistrationDTO extractOAuthPendingRegistration(String token) {
        Claims claims = parseClaims(token);
        String type = claims.get("type", String.class);
        if (!OAUTH_REGISTRATION_TYPE.equals(type)) {
            throw new IllegalArgumentException("Invalid registration token type");
        }

        String email = claims.get("email", String.class);
        String name = claims.get("name", String.class);
        if (email == null || email.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid registration token payload");
        }

        return new OAuthPendingRegistrationDTO(email, name);
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String generateUploadToken(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .claim("type", UPLOAD_TYPE)
                .issuedAt(new Date(now))
                .expiration(new Date(now + UPLOAD_EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    public boolean isValidUploadToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get("type", String.class);
            return UPLOAD_TYPE.equals(type) && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
