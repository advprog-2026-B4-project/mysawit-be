package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.out.PanenRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenStatus;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PanenRepositoryAdapter implements PanenRepositoryPort {

    private final PanenJpaRepository jpaRepository;
    private final PanenMapper mapper;

    @Override
    public PanenDTO save(PanenDTO dto) {
        // DTO → Domain → Entity → save → Entity → Domain → DTO
        var domain = mapper.dtoToDomain(dto);
        var entity = mapper.toEntity(domain);
        var saved = jpaRepository.save(entity);
        return mapper.entityToDto(saved);
    }

    @Override
    public PanenDTO findById(UUID id) {
        // Mengembalikan null sesuai aturan "API Conventions" di README/agent.md
        // agar Use Case yang melempar EntityNotFoundException, bukan layer ini.
        return jpaRepository.findByIdWithPhotos(id)
                .map(mapper::entityToDto)
                .orElse(null);
    }

    @Override
    public boolean existsByBuruhIdAndDate(UUID buruhId, LocalDate date) {
        return jpaRepository.existsByBuruhIdAndHarvestDate(buruhId, date);
    }

    @Override
    public List<PanenDTO> findByKebunIdAndStatus(UUID kebunId, String status) {
        PanenStatus panenStatus = status != null ? PanenStatus.valueOf(status.toUpperCase()) : null;
        return jpaRepository.findByKebunIdAndStatus(kebunId, panenStatus).stream()
                .map(mapper::entityToDto)
                .toList();
    }

    @Override
    public List<PanenDTO> findByBuruhId(UUID buruhId, LocalDate startDate, LocalDate endDate, String status) {
        PanenStatus panenStatus = (status != null && !status.isBlank()) ? PanenStatus.valueOf(status.toUpperCase()) : null;
        return jpaRepository.findByBuruhIdWithFilters(buruhId, startDate, endDate, panenStatus).stream()
                .map(mapper::entityToDto)
                .toList();
    }

    @Override
    public List<PanenDTO> findByKebunIdAndDate(UUID kebunId, LocalDate date) {
        return jpaRepository.findByKebunIdAndDateFilter(kebunId, date).stream()
                .map(mapper::entityToDto)
                .toList();
    }

    @Override
    public List<PanenDTO> findAllWithFilters(String status, LocalDate startDate, LocalDate endDate) {
        PanenStatus panenStatus = (status != null && !status.isBlank()) ? PanenStatus.valueOf(status.toUpperCase()) : null;
        return jpaRepository.findAllWithFilters(panenStatus, startDate, endDate)
                .stream()
                .map(mapper::entityToDto)
                .toList();
    }

    @Override
    public List<PanenDTO> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jpaRepository.findAllByIdsWithPhotos(ids).stream()
                .map(mapper::entityToDto)
                .toList();
    }

    @Override
    public PanenPageDTO findAllWithFiltersPaginated(String status, LocalDate startDate, LocalDate endDate, int page, int size) {
        PanenStatus panenStatus = (status != null && !status.isBlank()) ? PanenStatus.valueOf(status.toUpperCase()) : null;

        // Query 1: fetch IDs with pagination (avoids HHH90003004 from LEFT JOIN FETCH + Pageable)
        Page<UUID> idPage = jpaRepository.findAllIdsByFilters(panenStatus, startDate, endDate, PageRequest.of(page, size));
        if (idPage.isEmpty()) {
            return new PanenPageDTO(List.of(), page, size, 0L, 0, false, false);
        }

        // Query 2: fetch full entities (with photos) for the IDs on this page
        List<PanenDTO> dtos = jpaRepository.findAllByIdsWithPhotos(idPage.getContent()).stream()
                .map(mapper::entityToDto)
                .toList();

        return new PanenPageDTO(
                dtos,
                page,
                size,
                idPage.getTotalElements(),
                idPage.getTotalPages(),
                idPage.hasNext(),
                idPage.hasPrevious()
        );
    }
}
