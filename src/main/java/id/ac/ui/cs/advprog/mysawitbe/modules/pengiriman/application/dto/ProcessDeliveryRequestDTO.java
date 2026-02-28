package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProcessDeliveryRequestDTO(
    @NotNull(message = "Accepted weight is required")
    @Positive(message = "Accepted weight must be positive")
    Integer acceptedWeight,

    @NotBlank(message = "Status is required")
    String status,

    String reason
) {}
