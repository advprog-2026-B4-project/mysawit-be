package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoordinateEmbeddableTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        CoordinateEmbeddable coord = new CoordinateEmbeddable();
        coord.setLat(15);
        coord.setLng(25);

        assertThat(coord.getLat()).isEqualTo(15);
        assertThat(coord.getLng()).isEqualTo(25);
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        CoordinateEmbeddable coord = new CoordinateEmbeddable(10, 20);

        assertThat(coord.getLat()).isEqualTo(10);
        assertThat(coord.getLng()).isEqualTo(20);
    }
}
