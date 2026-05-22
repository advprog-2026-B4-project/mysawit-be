package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AssignDeliveryRequestDTO(
    @NotNull(message = "Supir ID is required")
    UUID supirId,

    @NotEmpty(message = "Panen IDs are required")
    List<UUID> panenIds
) {}
