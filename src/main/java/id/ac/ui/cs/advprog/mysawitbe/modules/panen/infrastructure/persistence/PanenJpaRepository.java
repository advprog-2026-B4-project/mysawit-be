package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    // Pencarian dengan filter tanggal (digunakan untuk fallback data Mandor)
    @Query("SELECT p FROM PanenEntity p WHERE (CAST(:date AS java.time.LocalDate) IS NULL OR p.harvestDate = :date)")
    List<PanenEntity> findAllWithDateFilter(@Param("date") LocalDate date);

    @Query("SELECT p FROM PanenEntity p WHERE p.kebunId = :kebunId " +
       "AND (CAST(:date AS java.time.LocalDate) IS NULL OR p.harvestDate = :date)")
List<PanenEntity> findByKebunIdAndDateFilter(@Param("kebunId") UUID kebunId, 
                                              @Param("date") LocalDate date);

    @Query("SELECT p FROM PanenEntity p WHERE p.kebunId = :kebunId")
    List<PanenEntity> findByKebunId(@Param("kebunId") UUID kebunId);
}