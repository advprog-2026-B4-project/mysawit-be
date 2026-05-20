package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.domain.PengirimanStatus;

import java.util.List;
import java.util.UUID;

/**
 * Use case interface for pengiriman write operations.
 */
public interface PengirimanCommandUseCase {

    /**
     * Mandor assigns a supir for a delivery of approved panen entries.
     * panenIds: list of approved panen IDs to include (total weight <= 400 kg enforced).
     */
    PengirimanDTO assignSupirForDelivery(UUID mandorId, UUID supirId, List<UUID> panenIds);

    /**
     * Supir updates the delivery status (e.g. IN_TRANSIT -> TIBA).
     * Publishes PengirimanStatusTibaEvent when status becomes TIBA.
     */
    PengirimanDTO updateDeliveryStatus(UUID pengirimanId, UUID supirId, PengirimanStatus newStatus);

    /**
     * Mandor approves delivery after supir marks it TIBA.
     * Publishes PengirimanApprovedByMandorEvent.
     */
    PengirimanDTO mandorApproveDelivery(UUID pengirimanId, UUID mandorId);

    /**
     * Mandor rejects delivery with a reason.
     */
    PengirimanDTO mandorRejectDelivery(UUID pengirimanId, UUID mandorId, String reason);

    /**
     * Admin processes delivery: full approve, partial accept, or reject.
     * acceptedWeight: actual accepted weight in grams (0 = full reject).
     * Publishes PengirimanProcessedByAdminEvent.
     */
    PengirimanDTO adminProcessDelivery(
            UUID pengirimanId,
            UUID adminId,
            int acceptedWeight,
            PengirimanStatus status,
            String reason
    );
}
