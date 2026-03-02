package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Immutable data transfer object for panen (harvest) data.
 * weight: stored in grams to maintain precision.
 * status: e.g. PENDING, APPROVED, REJECTED.
 * photoUrls: list of Cloudflare R2 public URLs.
 */
public record PanenDTO(
        UUID panenId,
        UUID buruhId,
        String buruhName,
        UUID kebunId,
        int weight,
        String status,
        List<String> photoUrls,
        LocalDateTime timestamp
) {}
