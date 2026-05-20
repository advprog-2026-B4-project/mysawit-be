package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for pengiriman persistence.
 * Implemented by infrastructure/persistence/PengirimanJpaAdapter.
 */
public interface PengirimanRepositoryPort {

    PengirimanDTO save(PengirimanDTO pengirimanDTO);

    PengirimanDTO findById(UUID pengirimanId);

    List<UUID> findAssignedPanenIds(List<UUID> panenIds);

    List<PengirimanDTO> findBySupirId(UUID supirId, LocalDate startDate, LocalDate endDate);

    List<PengirimanDTO> findActiveByMandorId(UUID mandorId);

    List<PengirimanDTO> findByMandorIdAndSupirId(UUID mandorId, UUID supirId);

    List<PengirimanDTO> findApprovedByMandorForAdmin(String mandorName, LocalDate date);
}
