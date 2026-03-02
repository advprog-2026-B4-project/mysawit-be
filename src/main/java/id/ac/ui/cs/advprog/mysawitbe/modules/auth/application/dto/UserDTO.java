package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto;

import java.util.UUID;

/**
 * Immutable data transfer object for user data.
 * role: one of ADMIN, MANDOR, BURUH, SUPIR
 */
public record UserDTO(
        UUID userId,
        String username,
        String name,
        String role,
        String email
) {}
