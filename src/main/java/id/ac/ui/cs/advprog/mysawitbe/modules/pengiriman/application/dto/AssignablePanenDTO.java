package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight view of approved panen that can be assigned into a delivery.
 * weight is stored in grams for consistency with the panen module.
 */
public record AssignablePanenDTO(
        UUID panenId,
        UUID buruhId,
        String buruhName,
        String description,
        int weight,
        LocalDateTime timestamp
) {}
