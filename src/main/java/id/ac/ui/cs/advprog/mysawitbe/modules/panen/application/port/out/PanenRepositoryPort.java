package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenPageDTO;

/**
 * Outbound port for panen persistence.
 * Implemented by infrastructure/persistence/PanenJpaAdapter.
 */
public interface PanenRepositoryPort {

    PanenDTO save(PanenDTO panenDTO);

    PanenDTO findById(UUID panenId);

    boolean existsByBuruhIdAndDate(UUID buruhId, LocalDate date);

    List<PanenDTO> findByKebunIdAndStatus(UUID kebunId, String status);

    List<PanenDTO> findByBuruhId(UUID buruhId, LocalDate startDate, LocalDate endDate, String status);

    List<PanenDTO> findByKebunIdAndDate(UUID kebunId, LocalDate date);

    List<PanenDTO> findAllWithFilters(String status, LocalDate startDate, LocalDate endDate);

    List<PanenDTO> findAllByIds(Collection<UUID> ids);

    PanenPageDTO findAllWithFiltersPaginated(String status, LocalDate startDate, LocalDate endDate, int page, int size);
}
