package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;

import java.util.List;
import java.util.UUID;

/**
 * Use case interface for kebun read/query operations.
 * Used as an internal interface by other modules.
 */
public interface KebunQueryUseCase {

    KebunDTO getKebunById(UUID kebunId);

    UUID getMandorIdByKebun(UUID kebunId);

    List<UserDTO> getSupirList(UUID kebunId);

    List<UserDTO> getBuruhList(UUID kebunId);

    /**
     * Returns all kebun, with optional search by nama or kode.
     */
    List<KebunDTO> listKebun(String searchNama, String searchKode);

    /**
     * Validate that a coordinate point does not fall inside any existing kebun polygon.
     */
    boolean validateCoordinates(int lat, int lng);
}
