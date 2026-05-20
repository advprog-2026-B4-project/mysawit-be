package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PengirimanJpaRepository extends JpaRepository<PengirimanJpaEntity, UUID> {

    List<PengirimanJpaEntity> findBySupirIdOrderByTimestampDesc(UUID supirId);

    List<PengirimanJpaEntity> findBySupirIdAndTimestampGreaterThanEqualOrderByTimestampDesc(
            UUID supirId,
            LocalDateTime timestamp
    );

    List<PengirimanJpaEntity> findBySupirIdAndTimestampLessThanEqualOrderByTimestampDesc(
            UUID supirId,
            LocalDateTime timestamp
    );

    List<PengirimanJpaEntity> findBySupirIdAndTimestampBetweenOrderByTimestampDesc(
            UUID supirId,
            LocalDateTime startTimestamp,
            LocalDateTime endTimestamp
    );

    List<PengirimanJpaEntity> findByMandorIdAndStatusInOrderByTimestampDesc(
            UUID mandorId,
            Collection<String> statuses
    );

    List<PengirimanJpaEntity> findByMandorIdAndSupirIdOrderByTimestampDesc(UUID mandorId, UUID supirId);

    List<PengirimanJpaEntity> findByStatusOrderByTimestampDesc(String status);

    List<PengirimanJpaEntity> findByStatusAndTimestampBetweenOrderByTimestampDesc(
            String status,
            LocalDateTime startTimestamp,
            LocalDateTime endTimestamp
    );

    @Query("""
            select item.panenId
            from PengirimanPanenItemJpaEntity item
            where item.panenId in :panenIds
            """)
    List<UUID> findAssignedPanenIds(@Param("panenIds") Collection<UUID> panenIds);
}
