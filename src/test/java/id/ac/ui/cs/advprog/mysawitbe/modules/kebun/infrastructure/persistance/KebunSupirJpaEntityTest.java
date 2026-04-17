package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KebunSupirJpaEntityTest {

    @Test
    void testSetAndGetId() {
        KebunSupirJpaEntity entity = new KebunSupirJpaEntity();
        UUID id = UUID.randomUUID();
        entity.setId(id);

        assertThat(entity.getId()).isEqualTo(id);
    }

    @Test
    void testSettersAndGetters() {
        KebunSupirJpaEntity entity = new KebunSupirJpaEntity();
        UUID kebunId = UUID.randomUUID();
        UUID supirId = UUID.randomUUID();

        entity.setKebunId(kebunId);
        entity.setSupirId(supirId);

        assertThat(entity.getKebunId()).isEqualTo(kebunId);
        assertThat(entity.getSupirId()).isEqualTo(supirId);
    }
}
