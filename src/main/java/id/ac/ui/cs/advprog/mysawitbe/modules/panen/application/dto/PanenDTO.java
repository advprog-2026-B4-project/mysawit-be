package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Immutable data transfer object for panen (harvest) data.
 * weight: stored in grams to maintain precision.
 * status: e.g. PENDING, APPROVED, REJECTED.
 * photos: list of photo references (ID + Cloudflare R2 URL).
 */
public record PanenDTO(
        UUID panenId,
        UUID buruhId,
        String buruhName,
        UUID kebunId,
        String description,
        int weight,
        String status,
        String rejectionReason,
        List<PhotoDTO> photos,
        LocalDateTime timestamp
) {
    /**
     * Nested record agar photo ID tidak hilang saat DTO ↔ Domain conversion.
     */
    public record PhotoDTO(UUID photoId, String url) {}
}
