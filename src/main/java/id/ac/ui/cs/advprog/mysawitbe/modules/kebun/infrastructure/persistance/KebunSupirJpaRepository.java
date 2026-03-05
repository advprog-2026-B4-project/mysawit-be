package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KebunSupirJpaRepository extends JpaRepository<KebunSupirJpaEntity, UUID> {

    List<KebunSupirJpaEntity> findByKebunId(UUID kebunId);

    Optional<KebunSupirJpaEntity> findBySupirId(UUID supirId);
}