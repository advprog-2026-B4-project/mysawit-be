package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequestDTO(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    String password,

    @NotBlank(message = "Name is required")
    String name,

    @NotNull(message = "Role is required")
    UserRole role
) {}
