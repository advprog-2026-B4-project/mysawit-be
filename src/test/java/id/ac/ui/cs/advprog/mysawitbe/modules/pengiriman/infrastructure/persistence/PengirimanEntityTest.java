package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PengirimanEntityTest {

    @Test
    void pengirimanJpaEntity_accessorsBuilderAndPrePersistWork() {
        UUID pengirimanId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        PengirimanJpaEntity entity = PengirimanJpaEntity.builder()
                .pengirimanId(pengirimanId)
                .supirId(supirId)
                .mandorId(mandorId)
                .status("ASSIGNED")
                .totalWeight(100000)
                .acceptedWeight(0)
                .statusReason("reason")
                .panenItems(new ArrayList<>())
                .build();

        entity.prePersist();

        assertThat(entity.getPengirimanId()).isEqualTo(pengirimanId);
        assertThat(entity.getSupirId()).isEqualTo(supirId);
        assertThat(entity.getMandorId()).isEqualTo(mandorId);
        assertThat(entity.getStatus()).isEqualTo("ASSIGNED");
        assertThat(entity.getTotalWeight()).isEqualTo(100000);
        assertThat(entity.getAcceptedWeight()).isZero();
        assertThat(entity.getStatusReason()).isEqualTo("reason");
        assertThat(entity.getTimestamp()).isNotNull();
        assertThat(entity.getPanenItems()).isEmpty();

        LocalDateTime explicitTimestamp = LocalDateTime.of(2026, 4, 1, 8, 0);
        entity.setTimestamp(explicitTimestamp);
        entity.prePersist();
        assertThat(entity.getTimestamp()).isEqualTo(explicitTimestamp);
    }

    @Test
    void pengirimanPanenItemJpaEntity_accessorsBuilderAndConstructorsWork() {
        UUID itemId = UUID.randomUUID();
        UUID panenId = UUID.randomUUID();
        PengirimanJpaEntity pengiriman = new PengirimanJpaEntity();

        PengirimanPanenItemJpaEntity item = PengirimanPanenItemJpaEntity.builder()
                .pengirimanItemId(itemId)
                .pengiriman(pengiriman)
                .panenId(panenId)
                .build();

        assertThat(item.getPengirimanItemId()).isEqualTo(itemId);
        assertThat(item.getPengiriman()).isSameAs(pengiriman);
        assertThat(item.getPanenId()).isEqualTo(panenId);

        PengirimanPanenItemJpaEntity viaConstructor =
                new PengirimanPanenItemJpaEntity(itemId, pengiriman, panenId);
        assertThat(viaConstructor.getPengirimanItemId()).isEqualTo(itemId);

        PengirimanPanenItemJpaEntity empty = new PengirimanPanenItemJpaEntity();
        empty.setPengirimanItemId(itemId);
        empty.setPengiriman(pengiriman);
        empty.setPanenId(panenId);
        assertThat(empty.getPengirimanItemId()).isEqualTo(itemId);
        assertThat(empty.getPengiriman()).isSameAs(pengiriman);
        assertThat(empty.getPanenId()).isEqualTo(panenId);
    }
}
