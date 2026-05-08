package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.adapter;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.domain.PengirimanStatus;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.PengirimanJpaEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.PengirimanJpaRepository;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.mapper.PengirimanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PengirimanRepositoryAdapterTest {

    @Mock
    private PengirimanJpaRepository jpaRepository;

    @Mock
    private PengirimanMapper mapper;

    private PengirimanRepositoryAdapter adapter;
    private UUID pengirimanId;
    private UUID supirId;
    private UUID mandorId;
    private PengirimanDTO dto;
    private PengirimanJpaEntity entity;

    @BeforeEach
    void setUp() {
        adapter = new PengirimanRepositoryAdapter(jpaRepository, mapper);
        pengirimanId = UUID.randomUUID();
        supirId = UUID.randomUUID();
        mandorId = UUID.randomUUID();
        dto = new PengirimanDTO(pengirimanId, supirId, mandorId, "ASSIGNED", 100000, 0, LocalDateTime.now());
        entity = PengirimanJpaEntity.builder()
                .pengirimanId(pengirimanId)
                .supirId(supirId)
                .mandorId(mandorId)
                .status("ASSIGNED")
                .totalWeight(100000)
                .acceptedWeight(0)
                .timestamp(dto.timestamp())
                .build();
    }

    @Test
    void save_mapsEntityThroughRepository() {
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        assertThat(adapter.save(dto)).isSameAs(dto);
    }

    @Test
    void findById_found_mapsEntity() {
        when(jpaRepository.findById(pengirimanId)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        assertThat(adapter.findById(pengirimanId)).isSameAs(dto);
    }

    @Test
    void findById_notFound_returnsNull() {
        when(jpaRepository.findById(pengirimanId)).thenReturn(Optional.empty());

        assertThat(adapter.findById(pengirimanId)).isNull();
    }

    @Test
    void findAssignedPanenIds_nullOrEmpty_returnsEmptyWithoutRepositoryCall() {
        assertThat(adapter.findAssignedPanenIds(null)).isEmpty();
        assertThat(adapter.findAssignedPanenIds(List.of())).isEmpty();
        verifyNoInteractions(jpaRepository);
    }

    @Test
    void findAssignedPanenIds_values_delegatesRepository() {
        UUID panenId = UUID.randomUUID();
        when(jpaRepository.findAssignedPanenIds(List.of(panenId))).thenReturn(List.of(panenId));

        assertThat(adapter.findAssignedPanenIds(List.of(panenId))).containsExactly(panenId);
    }

    @Test
    void findBySupirId_withoutDateFilter_usesPlainQuery() {
        when(jpaRepository.findBySupirIdOrderByTimestampDesc(supirId)).thenReturn(List.of(entity));
        when(mapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(adapter.findBySupirId(supirId, null, null)).containsExactly(dto);
    }

    @Test
    void findBySupirId_withStartAndEnd_usesBetweenQuery() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 2);
        when(jpaRepository.findBySupirIdAndTimestampBetweenOrderByTimestampDesc(
                supirId, start.atStartOfDay(), end.plusDays(1).atStartOfDay().minusNanos(1)))
                .thenReturn(List.of(entity));
        when(mapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(adapter.findBySupirId(supirId, start, end)).containsExactly(dto);
    }

    @Test
    void findBySupirId_withStartOnly_usesGreaterThanQuery() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        when(jpaRepository.findBySupirIdAndTimestampGreaterThanEqualOrderByTimestampDesc(supirId, start.atStartOfDay()))
                .thenReturn(List.of(entity));
        when(mapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(adapter.findBySupirId(supirId, start, null)).containsExactly(dto);
    }

    @Test
    void findBySupirId_withEndOnly_usesLessThanQuery() {
        LocalDate end = LocalDate.of(2026, 4, 2);
        when(jpaRepository.findBySupirIdAndTimestampLessThanEqualOrderByTimestampDesc(
                supirId, end.plusDays(1).atStartOfDay().minusNanos(1)))
                .thenReturn(List.of(entity));
        when(mapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(adapter.findBySupirId(supirId, null, end)).containsExactly(dto);
    }

    @Test
    void findActiveByMandorId_usesActiveStatuses() {
        when(jpaRepository.findByMandorIdAndStatusInOrderByTimestampDesc(any(), any())).thenReturn(List.of(entity));
        when(mapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(adapter.findActiveByMandorId(mandorId)).containsExactly(dto);
        verify(jpaRepository).findByMandorIdAndStatusInOrderByTimestampDesc(
                mandorId,
                List.of(PengirimanStatus.ASSIGNED.name(), PengirimanStatus.IN_TRANSIT.name(), PengirimanStatus.TIBA.name())
        );
    }

    @Test
    void findByMandorIdAndSupirId_delegatesRepository() {
        when(jpaRepository.findByMandorIdAndSupirIdOrderByTimestampDesc(mandorId, supirId)).thenReturn(List.of(entity));
        when(mapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(adapter.findByMandorIdAndSupirId(mandorId, supirId)).containsExactly(dto);
    }

    @Test
    void findApprovedByMandorForAdmin_withoutDate_usesStatusQuery() {
        when(jpaRepository.findByStatusOrderByTimestampDesc(PengirimanStatus.APPROVED_MANDOR.name())).thenReturn(List.of(entity));
        when(mapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(adapter.findApprovedByMandorForAdmin("ignored", null)).containsExactly(dto);
    }

    @Test
    void findApprovedByMandorForAdmin_withDate_usesDateQuery() {
        LocalDate date = LocalDate.of(2026, 4, 1);
        when(jpaRepository.findByStatusAndTimestampBetweenOrderByTimestampDesc(
                PengirimanStatus.APPROVED_MANDOR.name(),
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)))
                .thenReturn(List.of(entity));
        when(mapper.toDtoList(List.of(entity))).thenReturn(List.of(dto));

        assertThat(adapter.findApprovedByMandorForAdmin("ignored", date)).containsExactly(dto);
    }
}
