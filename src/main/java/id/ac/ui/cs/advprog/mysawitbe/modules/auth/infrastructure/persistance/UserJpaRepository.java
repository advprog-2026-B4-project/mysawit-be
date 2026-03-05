package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    List<UserJpaEntity> findByRole(String role);

    List<UserJpaEntity> findByMandorId(UUID mandorId);

    boolean existsByEmail(String email);
}
