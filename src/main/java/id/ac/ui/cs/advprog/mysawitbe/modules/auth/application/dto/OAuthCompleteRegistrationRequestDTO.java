package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OAuthCompleteRegistrationRequestDTO(
        @NotBlank(message = "Registration token is required")
        String registrationToken,

        @NotNull(message = "Role is required")
        UserRole role,

        @Size(max = 100, message = "Nomor sertifikasi mandor maksimal 100 karakter")
        String mandorCertificationNumber
) {
}
