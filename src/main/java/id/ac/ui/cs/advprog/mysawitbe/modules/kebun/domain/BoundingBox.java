package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.domain;

public record BoundingBox(int minLat, int minLng, int maxLat, int maxLng) {

    public boolean containsPoint(int lat, int lng) {
        return lat > minLat && lat < maxLat && lng > minLng && lng < maxLng;
    }

    /**
     * True if intersection area is positive.
     * Touching edges is NOT considered overlap.
     */
    public boolean overlaps(BoundingBox other) {
        boolean separated =
                this.maxLat <= other.minLat ||
                        other.maxLat <= this.minLat ||
                        this.maxLng <= other.minLng ||
                        other.maxLng <= this.minLng;

        return !separated;
    }
}