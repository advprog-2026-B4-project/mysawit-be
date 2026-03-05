package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.out.UserRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.domain.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class UserJpaAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;
    private final UserMapper        mapper;

    public UserJpaAdapter(UserJpaRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper     = mapper;
    }

    @Override
    public UserDTO save(UserDTO userDTO, String hashedPassword) {
        User domain = mapper.toDomain(userDTO);
        domain.setHashedPassword(hashedPassword);

        UserJpaEntity entity;
        if (userDTO.userId() != null) {
            entity = repository.findById(userDTO.userId())
                    .orElse(mapper.toEntity(domain));
            entity.setName(domain.getName());
            entity.setEmail(domain.getEmail());
            entity.setRole(domain.getRole().name());
            if (hashedPassword != null) {
                entity.setPassword(hashedPassword);
            }
        } else {
            entity = mapper.toEntity(domain);
        }

        UserJpaEntity saved = repository.save(entity);
        return mapper.toDTO(mapper.toDomain(saved));
    }

    @Override
    public UserDTO findById(UUID userId) {
        return repository.findById(userId)
                .map(e -> mapper.toDTO(mapper.toDomain(e)))
                .orElse(null);
    }

    @Override
    public UserDTO findByEmail(String email) {
        return repository.findByEmail(email)
                .map(e -> mapper.toDTO(mapper.toDomain(e)))
                .orElse(null);
    }

    @Override
    public String findPasswordHashByEmail(String email) {
        return repository.findByEmail(email)
                .map(UserJpaEntity::getPassword)
                .orElse(null);
    }

    @Override
    public List<UserDTO> findAll() {
        return repository.findAll().stream()
                .map(e -> mapper.toDTO(mapper.toDomain(e)))
                .toList();
    }

    @Override
    public List<UserDTO> findByRole(String role) {
        return repository.findByRole(role).stream()
                .map(e -> mapper.toDTO(mapper.toDomain(e)))
                .toList();
    }

    @Override
    public List<UserDTO> findBuruhByMandorId(UUID mandorId) {
        return repository.findByMandorId(mandorId).stream()
                .map(e -> mapper.toDTO(mapper.toDomain(e)))
                .toList();
    }

    @Override
    public void deleteById(UUID userId) {
        repository.deleteById(userId);
    }

    @Override
    public boolean existsById(UUID userId) {
        return repository.existsById(userId);
    }

    @Override
    public void saveBuruhMandorAssignment(UUID buruhId, UUID mandorId) {
        repository.findById(buruhId).ifPresent(e -> {
            e.setMandorId(mandorId);
            repository.save(e);
        });
    }

    @Override
    public void removeBuruhMandorAssignment(UUID buruhId) {
        repository.findById(buruhId).ifPresent(e -> {
            e.setMandorId(null);
            repository.save(e);
        });
    }
}