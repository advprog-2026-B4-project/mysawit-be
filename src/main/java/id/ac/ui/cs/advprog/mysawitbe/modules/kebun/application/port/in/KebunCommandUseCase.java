package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

/**
 * Use case interface for kebun write operations.
 */
public interface KebunCommandUseCase {

    /**
     * Create a new kebun.
     * Must validate that coordinates do not overlap with existing kebun polygons.
     */
    @PreAuthorize("hasRole('ADMIN')")
    KebunDTO createKebun(String nama, String kode, int luas, List<CoordinateDTO> coordinates);

    /**
     * Edit an existing kebun. kebunId is immutable.
     */
    @PreAuthorize("hasRole('ADMIN')")
    KebunDTO editKebun(UUID kebunId, String nama, int luas, List<CoordinateDTO> coordinates);

    /**
     * Delete a kebun.
     * Fails if a mandor is still assigned to this kebun.
     */
    @PreAuthorize("hasRole('ADMIN')")
    void deleteKebun(UUID kebunId);

    /**
     * Assign a mandor to a kebun (1 mandor : 1 kebun invariant enforced).
     * Publishes MandorAssignedToKebunEvent.
     */
    @PreAuthorize("hasRole('ADMIN')")
    void assignMandorToKebun(UUID mandorId, UUID kebunId);

    /**
     * Move mandor from current kebun to a new kebun.
     * Old kebun must be reassigned immediately (cannot be left without mandor).
     */
    @PreAuthorize("hasRole('ADMIN')")
    void moveMandorToKebun(UUID mandorId, UUID newKebunId);

    /**
     * Assign a supir to a kebun (many supir : 1 kebun allowed).
     */
    @PreAuthorize("hasRole('ADMIN')")
    void assignSupirToKebun(UUID supirId, UUID kebunId);

    /**
     * Move supir to a different kebun.
     */
    @PreAuthorize("hasRole('ADMIN')")
    void moveSupirToKebun(UUID supirId, UUID newKebunId);
}