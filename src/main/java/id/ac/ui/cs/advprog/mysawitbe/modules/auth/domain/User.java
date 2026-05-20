package id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {
     private UUID userId;
    private String username;
    private String email;
    private String name;
    private String hashedPassword;   // null for OAuth-only accounts
    private UserRole role;
    private UUID mandorId;           // non-null only when role == BURUH
    private String mandorCertificationNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public User(UUID userId, String username, String email, String name,
                String hashedPassword, UserRole role, UUID mandorId,
                String mandorCertificationNumber,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.name = name;
        this.hashedPassword = hashedPassword;
        this.role = role;
        this.mandorId = mandorId;
        this.mandorCertificationNumber = mandorCertificationNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // true when the accoung was created via Google OAuth (no local pass)
    public boolean isOAuthAccount() { return hashedPassword == null; }

    // True when user holds admin role
    public boolean isAdmin() { return UserRole.ADMIN.equals(role); }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public UUID getMandorId() { return mandorId; }
    public void setMandorId(UUID mandorId) { this.mandorId = mandorId; }

    public String getMandorCertificationNumber() { return mandorCertificationNumber; }
    public void setMandorCertificationNumber(String mandorCertificationNumber) {
        this.mandorCertificationNumber = mandorCertificationNumber;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
