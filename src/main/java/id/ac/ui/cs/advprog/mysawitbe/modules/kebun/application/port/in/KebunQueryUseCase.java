package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;

/**
 * Use case interface for kebun read/query operations.
 * Used as an internal interface by other modules.
 */
public interface KebunQueryUseCase {

    @PreAuthorize("hasRole('ADMIN')")
    KebunDTO getKebunById(UUID kebunId);

    @PreAuthorize("hasRole('ADMIN')")
    UUID getMandorIdByKebun(UUID kebunId);

    @PreAuthorize("hasRole('ADMIN')")
    List<UserDTO> getSupirList(UUID kebunId);

    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANDOR') and #mandorId.toString() == authentication.name)")
    List<UserDTO> getSupirListByMandorId(UUID mandorId);

    @PreAuthorize("hasRole('ADMIN')")
    List<UserDTO> getBuruhList(UUID kebunId);

    /**
     * Returns all kebun, with optional search by nama or kode.
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<KebunDTO> listKebun(String searchNama, String searchKode);

    /**
     * Validate that a coordinate point does not fall inside any existing kebun polygon.
     */
    @PreAuthorize("hasRole('ADMIN')")
    boolean validateCoordinates(int lat, int lng);

    UUID findKebunIdByMandorId(UUID mandorId);
}
