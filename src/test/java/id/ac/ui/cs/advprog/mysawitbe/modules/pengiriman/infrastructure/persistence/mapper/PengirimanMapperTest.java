package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.mapper;

import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.PengirimanJpaEntity;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence.PengirimanPanenItemJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PengirimanMapperTest {

    private final PengirimanMapper mapper = new PengirimanMapperImpl();

    @Test
    void toDto_mapsEntityAndPanenItems() {
        UUID pengirimanId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 1, 8, 0);
        PengirimanJpaEntity entity = PengirimanJpaEntity.builder()
                .pengirimanId(pengirimanId)
                .supirId(supirId)
                .mandorId(mandorId)
                .status("ASSIGNED")
                .totalWeight(100000)
                .acceptedWeight(0)
                .statusReason("reason")
                .timestamp(timestamp)
                .panenItems(List.of(PengirimanPanenItemJpaEntity.builder().panenId(panenId).build()))
                .build();

        PengirimanDTO dto = mapper.toDto(entity);

        assertThat(dto.pengirimanId()).isEqualTo(pengirimanId);
        assertThat(dto.supirId()).isEqualTo(supirId);
        assertThat(dto.mandorId()).isEqualTo(mandorId);
        assertThat(dto.status()).isEqualTo("ASSIGNED");
        assertThat(dto.totalWeight()).isEqualTo(100000);
        assertThat(dto.acceptedWeight()).isZero();
        assertThat(dto.statusReason()).isEqualTo("reason");
        assertThat(dto.panenIds()).containsExactly(panenId);
        assertThat(dto.timestamp()).isEqualTo(timestamp);
        assertThat(dto.supirName()).isNull();
        assertThat(dto.mandorName()).isNull();
    }

    @Test
    void toDto_withNullEntity_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toEntity_mapsDtoAndAttachesParent() {
        UUID pengirimanId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 1, 8, 0);
        PengirimanDTO dto = new PengirimanDTO(
                pengirimanId,
                supirId,
                "Ignored Supir",
                mandorId,
                "Ignored Mandor",
                "ASSIGNED",
                100000,
                0,
                "reason",
                List.of(panenId),
                timestamp
        );

        PengirimanJpaEntity entity = mapper.toEntity(dto);

        assertThat(entity.getPengirimanId()).isEqualTo(pengirimanId);
        assertThat(entity.getSupirId()).isEqualTo(supirId);
        assertThat(entity.getMandorId()).isEqualTo(mandorId);
        assertThat(entity.getStatus()).isEqualTo("ASSIGNED");
        assertThat(entity.getTotalWeight()).isEqualTo(100000);
        assertThat(entity.getAcceptedWeight()).isZero();
        assertThat(entity.getStatusReason()).isEqualTo("reason");
        assertThat(entity.getTimestamp()).isEqualTo(timestamp);
        assertThat(entity.getPanenItems()).singleElement().satisfies(item -> {
            assertThat(item.getPanenId()).isEqualTo(panenId);
            assertThat(item.getPengiriman()).isSameAs(entity);
        });
    }

    @Test
    void toEntity_withNullDto_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDtoList_mapsNullAndNonNullList() {
        PengirimanJpaEntity entity = PengirimanJpaEntity.builder()
                .pengirimanId(UUID.randomUUID())
                .supirId(UUID.randomUUID())
                .mandorId(UUID.randomUUID())
                .status("ASSIGNED")
                .totalWeight(1)
                .acceptedWeight(0)
                .timestamp(LocalDateTime.now())
                .panenItems(List.of())
                .build();

        assertThat(mapper.toDtoList(null)).isNull();
        assertThat(mapper.toDtoList(List.of(entity))).hasSize(1);
    }

    @Test
    void defaultCollectionMappers_handleNullAndValues() {
        UUID panenId = UUID.randomUUID();
        assertThat(mapper.mapPanenItems(null)).isEmpty();
        assertThat(mapper.mapPanenIds(null)).isEmpty();
        assertThat(mapper.mapPanenItems(List.of(PengirimanPanenItemJpaEntity.builder().panenId(panenId).build())))
                .containsExactly(panenId);
        assertThat(mapper.mapPanenIds(List.of(panenId)))
                .singleElement()
                .satisfies(item -> assertThat(item.getPanenId()).isEqualTo(panenId));
    }

    @Test
    void attachParent_withNullPanenItems_isNoop() {
        PengirimanJpaEntity entity = new PengirimanJpaEntity();
        entity.setPanenItems(null);

        mapper.attachParent(entity);

        assertThat(entity.getPanenItems()).isNull();
    }
}
