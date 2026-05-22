package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenStatus;

public interface PanenJpaRepository extends JpaRepository<PanenEntity, UUID> {

    @Query("SELECT DISTINCT p FROM PanenEntity p LEFT JOIN FETCH p.photos WHERE p.panenId = :panenId")
    Optional<PanenEntity> findByIdWithPhotos(@Param("panenId") UUID panenId);

    boolean existsByBuruhIdAndHarvestDate(UUID buruhId, LocalDate harvestDate);

    List<PanenEntity> findByKebunIdAndStatus(UUID kebunId, PanenStatus status);

    @Query("SELECT p FROM PanenEntity p WHERE p.buruhId = :buruhId " +
       "AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR p.harvestDate >= :startDate) " +
       "AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR p.harvestDate <= :endDate) " +
       "AND (:status IS NULL OR p.status = :status)")
    List<PanenEntity> findByBuruhIdWithFilters(@Param("buruhId") UUID buruhId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("status") PanenStatus status);

    @Query("SELECT p FROM PanenEntity p WHERE (CAST(:date AS java.time.LocalDate) IS NULL OR p.harvestDate = :date)")
    List<PanenEntity> findAllWithDateFilter(@Param("date") LocalDate date);

    @Query("SELECT p FROM PanenEntity p WHERE p.kebunId = :kebunId " +
       "AND (CAST(:date AS java.time.LocalDate) IS NULL OR p.harvestDate = :date)")
    List<PanenEntity> findByKebunIdAndDateFilter(@Param("kebunId") UUID kebunId,
                                                 @Param("date") LocalDate date);

    @Query("SELECT p FROM PanenEntity p WHERE p.kebunId = :kebunId")
    List<PanenEntity> findByKebunId(@Param("kebunId") UUID kebunId);

    @Query("""
      SELECT DISTINCT p FROM PanenEntity p
      LEFT JOIN FETCH p.photos
      WHERE (:status IS NULL OR p.status = :status)
        AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR p.harvestDate >= :startDate)
        AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR p.harvestDate <= :endDate)
      ORDER BY p.createdAt DESC
    """)
    List<PanenEntity> findAllWithFilters(
            @Param("status") PanenStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT DISTINCT p FROM PanenEntity p LEFT JOIN FETCH p.photos WHERE p.panenId IN :ids")
    List<PanenEntity> findAllByIdsWithPhotos(@Param("ids") Collection<UUID> ids);

    // Two-query pagination: fetch IDs first (safe with Pageable + ORDER BY),
    // then fetch full entities by ID in a second query to avoid HHH90003004.
    @Query("""
      SELECT p.panenId FROM PanenEntity p
      WHERE (:status IS NULL OR p.status = :status)
        AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR p.harvestDate >= :startDate)
        AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR p.harvestDate <= :endDate)
      ORDER BY p.createdAt DESC
    """)
    Page<UUID> findAllIdsByFilters(
            @Param("status") PanenStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
