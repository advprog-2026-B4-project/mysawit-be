package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenStatus;

public interface PanenJpaRepository extends JpaRepository<PanenEntity, UUID> {
    
    boolean existsByBuruhIdAndHarvestDate(UUID buruhId, LocalDate harvestDate);

    List<PanenEntity> findByKebunIdAndStatus(UUID kebunId, PanenStatus status);

    // Filter dinamis untuk Buruh (Null-safe query)
    @Query("SELECT p FROM PanenEntity p WHERE p.buruhId = :buruhId " +
           "AND (:startDate IS NULL OR p.harvestDate >= :startDate) " +
           "AND (:endDate IS NULL OR p.harvestDate <= :endDate) " +
           "AND (:status IS NULL OR p.status = :status)")
    List<PanenEntity> findByBuruhIdWithFilters(@Param("buruhId") UUID buruhId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("status") PanenStatus status);

    // Pencarian dengan filter tanggal (digunakan untuk fallback data Mandor)
    @Query("SELECT p FROM PanenEntity p WHERE (:date IS NULL OR p.harvestDate = :date)")
    List<PanenEntity> findAllWithDateFilter(@Param("date") LocalDate date);
}