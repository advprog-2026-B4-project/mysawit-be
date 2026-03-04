package id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.UserRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserQueryUseCaseImpl implements UserQueryUseCase {
    
    private final UserRepositoryPort userRepository;

    public UserQueryUseCaseImpl(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDTO getUserById(UUID userId) {
        UserDTO user = userRepository.findById(userId);
        if (user == null) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
        return user;
    }

    @Override
    public String getUserRole(UUID userId) {
        return getUserById(userId).role();
    }

    @Override
    public boolean verifyUserExists(UUID userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public List<UserDTO> getBuruhByMandorId(UUID mandorId) {
        return userRepository.findBuruhByMandorId(mandorId);
    }

    @Override
    public List<UserDTO> listUsers(String roleFilter) {
        if (roleFilter == null || roleFilter.isBlank()) {
            return userRepository.findAll();
        }
        return userRepository.findByRole(roleFilter.toUpperCase());
    }
}
