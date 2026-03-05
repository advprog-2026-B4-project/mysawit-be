package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;

import java.util.List;
import java.util.UUID;

/**
 * Use case interface for user read/query operations.
 * Used by other modules as an internal interface (mock at 0% checkpoint).
 */
public interface UserQueryUseCase {

    UserDTO getUserById(UUID userId);

    String getUserRole(UUID userId);

    boolean verifyUserExists(UUID userId);

    List<UserDTO> getBuruhByMandorId(UUID mandorId);

    /**
     * Returns all users, optionally filtered by role.
     * roleFilter: nullable; if null returns all.
     */
    List<UserDTO> listUsers(String roleFilter);

    List<UserDTO> listUsers(String roleFilter, String search);
}
