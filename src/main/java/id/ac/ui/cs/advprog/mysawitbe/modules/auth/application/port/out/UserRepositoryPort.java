package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for user persistence.
 * Implemented by infrastructure/persistence/UserJpaAdapter.
 */
public interface UserRepositoryPort {

    UserDTO save(UserDTO userDTO, String hashedPassword);

    UserDTO findById(UUID userId);

    UserDTO findByEmail(String email);

    /**
     * Find the hashed password by email for authentication.
     * Returns null if user not found.
     */
    String findPasswordHashByEmail(String email);

    List<UserDTO> findAll();

    List<UserDTO> findByRole(String role);

    List<UserDTO> findBuruhByMandorId(UUID mandorId);

    void deleteById(UUID userId);

    boolean existsById(UUID userId);

    void saveBuruhMandorAssignment(UUID buruhId, UUID mandorId);
}
