package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.domain.PengirimanStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryStatusRequestDTO(
    @NotNull(message = "New status is required")
    PengirimanStatus newStatus
) {}
