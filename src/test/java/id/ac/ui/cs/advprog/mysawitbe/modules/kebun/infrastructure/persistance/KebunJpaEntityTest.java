package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KebunJpaEntityTest {

    @Test
    void testAllArgsConstructorAndGetters() {
        UUID id = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        CoordinateEmbeddable coord = new CoordinateEmbeddable(1, 2);

        KebunJpaEntity entity = new KebunJpaEntity(id, "Nama Kebun", "KB-01", 100, mandorId, List.of(coord));

        assertThat(entity.getKebunId()).isEqualTo(id);
        assertThat(entity.getKode()).isEqualTo("KB-01");
        assertThat(entity.getLuas()).isEqualTo(100);
        assertThat(entity.getNama()).isEqualTo("Nama Kebun");
        assertThat(entity.getMandorId()).isEqualTo(mandorId);
        assertThat(entity.getCoordinates()).hasSize(1);
    }
}
