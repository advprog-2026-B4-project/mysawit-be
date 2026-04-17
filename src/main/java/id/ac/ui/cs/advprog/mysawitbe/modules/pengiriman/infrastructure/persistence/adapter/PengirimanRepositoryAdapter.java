package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out.PengirimanRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.domain.PengirimanStatus;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.PengirimanJpaEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.PengirimanJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.mapper.PengirimanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PengirimanRepositoryAdapter implements PengirimanRepositoryPort {

    private static final List<String> ACTIVE_STATUSES = List.of(
            PengirimanStatus.ASSIGNED.name(),
            PengirimanStatus.IN_TRANSIT.name(),
            PengirimanStatus.TIBA.name()
    );

    private final PengirimanJpaRepository jpaRepository;
    private final PengirimanMapper mapper;

    @Override
    public PengirimanDTO save(PengirimanDTO pengirimanDTO) {
        PengirimanJpaEntity entity = mapper.toEntity(pengirimanDTO);
        return mapper.toDto(jpaRepository.save(entity));
    }

    @Override
    public PengirimanDTO findById(UUID pengirimanId) {
        return jpaRepository.findById(pengirimanId)
                .map(mapper::toDto)
                .orElse(null);
    }

    @Override
    public List<UUID> findAssignedPanenIds(List<UUID> panenIds) {
        if (panenIds == null || panenIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAssignedPanenIds(panenIds);
    }

    @Override
    public List<PengirimanDTO> findBySupirId(UUID supirId, LocalDate startDate, LocalDate endDate) {
        List<PengirimanJpaEntity> entities;

        if (startDate != null && endDate != null) {
            entities = jpaRepository.findBySupirIdAndTimestampBetweenOrderByTimestampDesc(
                    supirId,
                    startDate.atStartOfDay(),
                    toEndOfDay(endDate)
            );
        } else if (startDate != null) {
            entities = jpaRepository.findBySupirIdAndTimestampGreaterThanEqualOrderByTimestampDesc(
                    supirId,
                    startDate.atStartOfDay()
            );
        } else if (endDate != null) {
            entities = jpaRepository.findBySupirIdAndTimestampLessThanEqualOrderByTimestampDesc(
                    supirId,
                    toEndOfDay(endDate)
            );
        } else {
            entities = jpaRepository.findBySupirIdOrderByTimestampDesc(supirId);
        }

        return mapper.toDtoList(entities);
    }

    @Override
    public List<PengirimanDTO> findActiveByMandorId(UUID mandorId) {
        return mapper.toDtoList(
                jpaRepository.findByMandorIdAndStatusInOrderByTimestampDesc(mandorId, ACTIVE_STATUSES)
        );
    }

    @Override
    public List<PengirimanDTO> findByMandorIdAndSupirId(UUID mandorId, UUID supirId) {
        return mapper.toDtoList(jpaRepository.findByMandorIdAndSupirIdOrderByTimestampDesc(mandorId, supirId));
    }

    @Override
    public List<PengirimanDTO> findApprovedByMandorForAdmin(String mandorName, LocalDate date) {
        List<PengirimanJpaEntity> entities;
        if (date == null) {
            entities = jpaRepository.findByStatusOrderByTimestampDesc(PengirimanStatus.APPROVED_MANDOR.name());
        } else {
            entities = jpaRepository.findByStatusAndTimestampBetweenOrderByTimestampDesc(
                    PengirimanStatus.APPROVED_MANDOR.name(),
                    date.atStartOfDay(),
                    toEndOfDay(date)
            );
        }
        return mapper.toDtoList(entities);
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay().minusNanos(1);
    }
}
