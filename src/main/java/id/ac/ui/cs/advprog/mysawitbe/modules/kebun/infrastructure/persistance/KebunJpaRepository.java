package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KebunJpaRepository extends JpaRepository<KebunJpaEntity, UUID> {

    List<KebunJpaEntity> findByNamaContainingIgnoreCaseOrKodeContainingIgnoreCase(String nama, String kode);

    Optional<KebunJpaEntity> findByMandorId(UUID mandorId);

    boolean existsByKode(String kode);
}