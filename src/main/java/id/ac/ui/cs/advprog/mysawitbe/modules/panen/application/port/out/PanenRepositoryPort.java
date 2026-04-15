package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;

/**
 * Outbound port for panen persistence.
 * Implemented by infrastructure/persistence/PanenJpaAdapter.
 */
public interface PanenRepositoryPort {

    PanenDTO save(PanenDTO panenDTO);

    PanenDTO findById(UUID panenId);

    /**
     * Check if buruh already has a panen entry for today (daily limit enforcement).
     */
    boolean existsByBuruhIdAndDate(UUID buruhId, LocalDate date);

    List<PanenDTO> findByKebunIdAndStatus(UUID kebunId, String status);

    List<PanenDTO> findByBuruhId(UUID buruhId, LocalDate startDate, LocalDate endDate, String status);

    List<PanenDTO> findByKebunIdAndDate(UUID kebunId, LocalDate date);
}
