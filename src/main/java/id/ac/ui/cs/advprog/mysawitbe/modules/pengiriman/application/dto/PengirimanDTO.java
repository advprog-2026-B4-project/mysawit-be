package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Immutable data transfer object for pengiriman (delivery) data.
 * totalWeight and acceptedWeight are stored in grams to stay consistent with panen/payroll.
 */
public record PengirimanDTO(
        UUID pengirimanId,
        UUID supirId,
        String supirName,
        UUID mandorId,
        String mandorName,
        String status,
        int totalWeight,
        int acceptedWeight,
        String statusReason,
        List<UUID> panenIds,
        LocalDateTime timestamp
) {
        public PengirimanDTO(
                UUID pengirimanId,
                UUID supirId,
                UUID mandorId,
                String status,
                int totalWeight,
                int acceptedWeight,
                LocalDateTime timestamp
        ) {
                this(
                        pengirimanId,
                        supirId,
                        null,
                        mandorId,
                        null,
                        status,
                        totalWeight,
                        acceptedWeight,
                        null,
                        List.of(),
                        timestamp
                );
        }
}
