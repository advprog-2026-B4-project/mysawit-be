package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Use case interface for pengiriman read/query operations.
 * Used as an internal interface by other modules.
 */
public interface PengirimanQueryUseCase {

    PengirimanDTO getPengirimanById(UUID pengirimanId);

    /**
     * Supir's delivery history, filterable by date.
     */
    List<PengirimanDTO> listDeliveriesBySupir(UUID supirId, LocalDate startDate, LocalDate endDate);

    /**
     * Mandor's active deliveries view.
     */
    List<PengirimanDTO> listActiveDeliveriesByMandor(UUID mandorId);

    /**
     * Mandor views deliveries of a specific supir (profile page).
     */
    List<PengirimanDTO> listDeliveriesOfSupirByMandor(UUID mandorId, UUID supirId);

    /**
     * Admin views all mandor-approved deliveries, filterable by mandor name and date.
     */
    List<PengirimanDTO> listApprovedDeliveriesForAdmin(String mandorName, LocalDate date);
}
