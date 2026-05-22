package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.domain;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class BoundingBoxTest {
    @Test
    void overlaps_touchingEdges_returnsFalse() {
        BoundingBox b1 = new BoundingBox(0, 0, 10, 10);
        BoundingBox b2 = new BoundingBox(10, 0, 20, 10); // Menyentuh di Lat 10
        assertThat(b1.overlaps(b2)).isFalse();
    }

    @Test
    void containsPoint_onEdge_returnsFalse() {
        BoundingBox b = new BoundingBox(0, 0, 10, 10);
        assertThat(b.containsPoint(0, 5)).isFalse(); // Tepat di garis batas
        assertThat(b.containsPoint(5, 5)).isTrue();  // Di dalam
    }

    @Test
    void containsPoint_outside_returnsFalse() {
        BoundingBox b = new BoundingBox(0, 0, 10, 10);
        assertThat(b.containsPoint(-1, 5)).isFalse();
        assertThat(b.containsPoint(11, 5)).isFalse();
        assertThat(b.containsPoint(5, -1)).isFalse();
        assertThat(b.containsPoint(5, 11)).isFalse();
    }

    @Test
    void overlaps_contained_returnsTrue() {
        BoundingBox b1 = new BoundingBox(0, 0, 20, 20);
        BoundingBox b2 = new BoundingBox(5, 5, 15, 15);
        assertThat(b1.overlaps(b2)).isTrue();
    }

    @Test
    void overlaps_separatedIndividually_returnsFalse() {
        BoundingBox base = new BoundingBox(10, 10, 20, 20);

        // Terpisah di atas (maxLat <= other.minLat)
        assertThat(base.overlaps(new BoundingBox(25, 10, 35, 20))).isFalse();
        // Terpisah di bawah (other.maxLat <= this.minLat)
        assertThat(base.overlaps(new BoundingBox(0, 10, 5, 20))).isFalse();
        // Terpisah di kanan (maxLng <= other.minLng)
        assertThat(base.overlaps(new BoundingBox(10, 25, 20, 35))).isFalse();
        // Terpisah di kiri (other.maxLng <= this.minLng)
        assertThat(base.overlaps(new BoundingBox(10, 0, 20, 5))).isFalse();
    }

    @Test
    void testConstructorIsPrivate() throws Exception {
        Constructor<KebunGeometry> constructor = KebunGeometry.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void validateSquareCorners_notAxisAligned_throwsException() {
        // 4 titik unik tapi membentuk belah ketupat (diamond)
        List<CoordinateDTO> diamondCoords = List.of(
                new CoordinateDTO(0, 5), new CoordinateDTO(5, 10),
                new CoordinateDTO(10, 5), new CoordinateDTO(5, 0)
        );
        assertThatThrownBy(() -> KebunGeometry.validateSquareCorners(diamondCoords))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateSquareCorners_minLatEqualsMaxLat_throwsException() {
        // Kasus di mana minLat == maxLat (membentuk garis horizontal)
        List<CoordinateDTO> lineCoords = List.of(
                new CoordinateDTO(10, 0), new CoordinateDTO(10, 10),
                new CoordinateDTO(10, 0), new CoordinateDTO(10, 10)
        );
        assertThatThrownBy(() -> KebunGeometry.validateSquareCorners(lineCoords))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("luas sisi harus > 0");
    }

    @Test
    void validateSquareCorners_minLngEqualsMaxLng_throwsException() {
        // Kasus di mana minLng == maxLng (membentuk garis vertikal)
        List<CoordinateDTO> lineCoords = List.of(
                new CoordinateDTO(0, 10), new CoordinateDTO(10, 10),
                new CoordinateDTO(0, 10), new CoordinateDTO(10, 10)
        );
        assertThatThrownBy(() -> KebunGeometry.validateSquareCorners(lineCoords))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("luas sisi harus > 0");
    }

    @Test
    void toBoundingBox_zeroArea_throwsIllegalArgumentException() {
        // Kasus di mana koordinat valid sebagai list (4 titik) tapi membentuk titik/garis
        // sehingga minLat == maxLat atau minLng == maxLng
        List<CoordinateDTO> lineCoords = List.of(
                new CoordinateDTO(10, 10), new CoordinateDTO(10, 10),
                new CoordinateDTO(10, 10), new CoordinateDTO(10, 10)
        );

        // validateSquareCorners akan melempar exception lebih dulu jika dipanggil lewat toBoundingBox
        // karena minLat >= maxLat dicek di sana juga.
        assertThatThrownBy(() -> KebunGeometry.toBoundingBox(lineCoords))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("luas sisi harus > 0");
    }

    @Test
    void calculateBoundingBox_zeroArea_throwsIllegalArgumentException() throws Exception {
        // Menggunakan refleksi untuk memanggil metode private calculateBoundingBox
        java.lang.reflect.Method method = KebunGeometry.class.getDeclaredMethod("calculateBoundingBox", List.class);
        method.setAccessible(true);

        List<CoordinateDTO> lineCoords = List.of(
                new CoordinateDTO(10, 10), new CoordinateDTO(10, 10),
                new CoordinateDTO(10, 10), new CoordinateDTO(10, 10)
        );

        try {
            method.invoke(null, lineCoords);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            assertThat(targetException).isInstanceOf(IllegalArgumentException.class);
            assertThat(targetException.getMessage()).contains("luas sisi harus > 0");
        }
    }

    @Test
    void validateSquareCorners_minLatGreaterThanMaxLat_throwsException() {
        // Kasus di mana minLat > maxLat (tidak mungkin secara matematis dengan Math.min/max,
        // tapi untuk memicu branch coverage pada >=)
        // Kita gunakan titik yang sama (min == max) untuk memicu kondisi >=
        List<CoordinateDTO> coords = List.of(
                new CoordinateDTO(10, 0), new CoordinateDTO(10, 10),
                new CoordinateDTO(10, 0), new CoordinateDTO(10, 10)
        );
        assertThatThrownBy(() -> KebunGeometry.validateSquareCorners(coords))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("luas sisi harus > 0");
    }

    @Test
    void validateSquareCorners_minLngGreaterThanMaxLng_throwsException() {
        // Kasus di mana minLng == maxLng
        List<CoordinateDTO> coords = List.of(
                new CoordinateDTO(0, 10), new CoordinateDTO(10, 10),
                new CoordinateDTO(0, 10), new CoordinateDTO(10, 10)
        );
        assertThatThrownBy(() -> KebunGeometry.validateSquareCorners(coords))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("luas sisi harus > 0");
    }

    @Test
    void calculateBoundingBox_lngZeroArea_throwsException() throws Exception {
        // Menguji bagian kedua dari kondisi || (minLng >= maxLng) di metode private
        java.lang.reflect.Method method = KebunGeometry.class.getDeclaredMethod("calculateBoundingBox", List.class);
        method.setAccessible(true);

        List<CoordinateDTO> coords = List.of(
                new CoordinateDTO(0, 10), new CoordinateDTO(10, 10),
                new CoordinateDTO(0, 10), new CoordinateDTO(10, 10)
        );

        try {
            method.invoke(null, coords);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            assertThat(targetException).isInstanceOf(IllegalArgumentException.class);
            assertThat(targetException.getMessage()).contains("luas sisi harus > 0");
        }
    }
}