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
        return listUsers(roleFilter, null);
    }

    @Override
    public List<UserDTO> listUsers(String roleFilter, String search) {
        List<UserDTO> users;
        if (roleFilter == null || roleFilter.isBlank()) {
            users = userRepository.findAll();
        } else {
            users = userRepository.findByRole(roleFilter.toUpperCase());
        }
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            users = users.stream()
                    .filter(u -> u.name().toLowerCase().contains(q)
                              || u.email().toLowerCase().contains(q))
                    .toList();
        }
        return users;
    }
}