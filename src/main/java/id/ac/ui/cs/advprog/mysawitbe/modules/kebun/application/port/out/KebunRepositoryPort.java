package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.out;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for kebun persistence.
 * Implemented by infrastructure/persistence/KebunJpaAdapter.
 */
public interface KebunRepositoryPort {

    KebunDTO save(KebunDTO kebunDTO);

    KebunDTO findById(UUID kebunId);

    List<KebunDTO> findAll();

    List<KebunDTO> findByNamaContainingOrKodeContaining(String nama, String kode);

    UUID findMandorIdByKebunId(UUID kebunId);

    List<UUID> findSupirIdsByKebunId(UUID kebunId);

    List<UUID> findBuruhIdsByKebunId(UUID kebunId);

    void deleteById(UUID kebunId);

    boolean hasMandorAssigned(UUID kebunId);

    /**
     * Returns all coordinate polygons stored for overlap checking.
     */
    List<List<CoordinateDTO>> findAllCoordinates();

    void assignMandor(UUID mandorId, UUID kebunId);

    void assignSupir(UUID supirId, UUID kebunId);

    void moveSupir(UUID supirId, UUID newKebunId);
}
