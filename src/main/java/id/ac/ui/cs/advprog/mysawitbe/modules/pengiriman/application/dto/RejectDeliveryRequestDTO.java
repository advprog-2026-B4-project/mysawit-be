package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectDeliveryRequestDTO(
        @NotBlank(message = "Reject reason is required")
        String reason
) {}
