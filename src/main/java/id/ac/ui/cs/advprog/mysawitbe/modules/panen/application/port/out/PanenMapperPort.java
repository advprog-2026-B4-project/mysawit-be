package id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.Panen;

/**
 * Port abstraction for mapping between Panen domain model and PanenDTO.
 *
 * Application layer depends on this port; the concrete MapStruct
 * implementation (PanenMapper) lives in infrastructure/persistence.
 *
 * Only domain ↔ DTO conversions are exposed here.
 * Entity-related mappings stay internal to infrastructure.
 */
public interface PanenMapperPort {

    /** Domain → DTO (photos mapped to List&lt;PhotoDTO&gt; with ID preserved) */
    PanenDTO toDTO(Panen panen);

    /** DTO → Domain (photos reconstructed with original IDs from PhotoDTO) */
    Panen dtoToDomain(PanenDTO dto);
}
