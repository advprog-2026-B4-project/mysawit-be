package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignedSupirDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignmentRecommendationDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.AssignablePanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanPageDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Use case interface for pengiriman read/query operations.
 * Used as an internal interface by other modules.
 */
public interface PengirimanQueryUseCase {

    PengirimanDTO getPengirimanById(UUID pengirimanId);

    List<PengirimanDTO> listDeliveriesBySupir(UUID supirId, LocalDate startDate, LocalDate endDate);

    List<AssignedSupirDTO> listAssignedSupirForMandor(UUID mandorId, String searchNama);

    List<AssignablePanenDTO> listAssignablePanenForMandor(UUID mandorId);

    AssignmentRecommendationDTO recommendAssignmentForMandor(UUID mandorId, Integer maxCapacity);

    List<PengirimanDTO> listActiveDeliveriesByMandor(UUID mandorId);

    List<PengirimanDTO> listDeliveriesOfSupirByMandor(UUID mandorId, UUID supirId);

    PengirimanPageDTO listApprovedDeliveriesForAdmin(String mandorName, LocalDate date, int page, int size);
}
