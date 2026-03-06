package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Use case interface for panen read/query operations.
 * Used as an internal interface by other modules.
 */
public interface PanenQueryUseCase {

    PanenDTO getPanenById(UUID panenId);

    /**
     * Returns approved harvest records for a specific kebun.
     */
    List<PanenDTO> getApprovedPanenByKebun(UUID kebunId);

    /**
     * Returns panen history for a specific buruh.
     * All filter parameters are nullable (null = no filter).
     */
    List<PanenDTO> listPanenByBuruh(UUID buruhId, LocalDate startDate, LocalDate endDate, String status);

    /**
     * Returns all panen in a kebun visible to a mandor, filterable by buruh name and date.
     */
    List<PanenDTO> listPanenByMandor(UUID mandorId, String buruhName, LocalDate date);
}
