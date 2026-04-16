package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProcessDeliveryRequestDTO(
    @NotNull(message = "Accepted weight is required")
    @Min(value = 0, message = "Accepted weight cannot be negative")
    Integer acceptedWeight,

    @NotBlank(message = "Status is required")
    String status,

    String reason
) {}
