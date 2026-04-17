package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoordinateJpaMapperTest {

    @Test
    void testToEmbeddableAndToDto() {
        CoordinateJpaMapperImpl mapper = new CoordinateJpaMapperImpl();

        CoordinateDTO dto = new CoordinateDTO(15, 25);
        CoordinateEmbeddable embeddable = mapper.toEmbeddable(dto);
        assertThat(embeddable.getLat()).isEqualTo(15);
        assertThat(embeddable.getLng()).isEqualTo(25);

        CoordinateDTO resultDto = mapper.toDto(embeddable);
        assertThat(resultDto.lat()).isEqualTo(15);
        assertThat(resultDto.lng()).isEqualTo(25);
    }

    @Test
    void testNullMapping() {
        CoordinateJpaMapperImpl mapper = new CoordinateJpaMapperImpl();
        
        assertThat(mapper.toEmbeddable(null)).isNull();
        assertThat(mapper.toDto(null)).isNull();
    }
}
