package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto;

/**
 * Request DTO for email/password login.
 */
public record LoginRequestDTO(
        String email,
        String password
) {}
