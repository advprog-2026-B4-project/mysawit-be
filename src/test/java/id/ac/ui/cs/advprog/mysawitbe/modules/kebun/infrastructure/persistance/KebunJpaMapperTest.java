package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.KebunDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KebunJpaMapperTest {

    @Test
    void testToEntityAndToDto() {
        KebunJpaMapperImpl mapper = new KebunJpaMapperImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(mapper, "coordinateJpaMapper", new CoordinateJpaMapperImpl());

        KebunDTO dto = new KebunDTO(UUID.randomUUID(), "Nama Kebun", "KB-01", 10, List.of(new CoordinateDTO(1, 1)));
        KebunJpaEntity entity = mapper.toEntity(dto);

        assertThat(entity.getNama()).isEqualTo("Nama Kebun");
        assertThat(entity.getCoordinates()).hasSize(1);

        KebunDTO resultDto = mapper.toDto(entity);
        assertThat(resultDto.nama()).isEqualTo("Nama Kebun");
        assertThat(resultDto.coordinates()).hasSize(1);
    }

    @Test
    void testNullMapping() {
        KebunJpaMapperImpl mapper = new KebunJpaMapperImpl();
        
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDto((KebunJpaEntity) null)).isNull();
    }
}
