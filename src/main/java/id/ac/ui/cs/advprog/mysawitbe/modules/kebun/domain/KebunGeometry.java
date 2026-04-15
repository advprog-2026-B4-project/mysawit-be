package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.domain;

import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.dto.CoordinateDTO;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class KebunGeometry {

    private KebunGeometry() {}

    public static BoundingBox toBoundingBox(List<CoordinateDTO> coords) {
        validateSquareCorners(coords);

        return calculateBoundingBox(coords);
    }

    private static BoundingBox calculateBoundingBox(List<CoordinateDTO> coords) {

        int minLat = Integer.MAX_VALUE, maxLat = Integer.MIN_VALUE;
        int minLng = Integer.MAX_VALUE, maxLng = Integer.MIN_VALUE;

        for (CoordinateDTO c : coords) {
            minLat = Math.min(minLat, c.lat());
            maxLat = Math.max(maxLat, c.lat());
            minLng = Math.min(minLng, c.lng());
            maxLng = Math.max(maxLng, c.lng());
        }

        if (minLat >= maxLat || minLng >= maxLng) {
            throw new IllegalArgumentException("Koordinat kebun tidak valid (luas sisi harus > 0)");
        }

        return new BoundingBox(minLat, minLng, maxLat, maxLng);
    }

    /**
     * Validates that 4 coordinates represent axis-aligned square corners.
     */
    public static void validateSquareCorners(List<CoordinateDTO> coords) {
        if (coords == null || coords.size() != 4) {
            throw new IllegalArgumentException("Koordinat kebun harus tepat 4 titik sudut");
        }

        int minLat = Integer.MAX_VALUE, maxLat = Integer.MIN_VALUE;
        int minLng = Integer.MAX_VALUE, maxLng = Integer.MIN_VALUE;

        for (CoordinateDTO c : coords) {
            minLat = Math.min(minLat, c.lat());
            maxLat = Math.max(maxLat, c.lat());
            minLng = Math.min(minLng, c.lng());
            maxLng = Math.max(maxLng, c.lng());
        }

        if (minLat >= maxLat || minLng >= maxLng) {
            throw new IllegalArgumentException("Koordinat kebun tidak valid (luas sisi harus > 0)");
        }

        int sideLat = maxLat - minLat;
        int sideLng = maxLng - minLng;
        if (sideLat != sideLng) {
            throw new IllegalArgumentException("Koordinat harus membentuk 4 sudut persegi (axis-aligned)");
        }

        Set<String> expected = Set.of(
                key(minLat, minLng),
                key(minLat, maxLng),
                key(maxLat, minLng),
                key(maxLat, maxLng)
        );

        Set<String> actual = new HashSet<>();
        for (CoordinateDTO c : coords) {
            actual.add(key(c.lat(), c.lng()));
        }

        if (actual.size() != 4 || !actual.equals(expected)) {
            throw new IllegalArgumentException("Koordinat harus membentuk 4 sudut persegi (axis-aligned)");
        }
    }

    private static String key(int lat, int lng) {
        return lat + ":" + lng;
    }
}