package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable data transfer object for pengiriman (delivery) data.
 * totalWeight: total weight loaded by supir (grams).
 * acceptedWeight: weight accepted by admin after verification (grams).
 * status: e.g. ASSIGNED, IN_TRANSIT, TIBA, APPROVED, REJECTED.
 */
public record PengirimanDTO(
        UUID pengirimanId,
        UUID supirId,
        UUID mandorId,
        String status,
        int totalWeight,
        int acceptedWeight,
        LocalDateTime timestamp
) {}
