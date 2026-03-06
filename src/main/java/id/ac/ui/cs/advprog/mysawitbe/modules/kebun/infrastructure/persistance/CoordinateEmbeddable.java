package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import jakarta.persistence.Embeddable;

@Embeddable
public class CoordinateEmbeddable {

    private int lat;
    private int lng;

    protected CoordinateEmbeddable() {}

    public CoordinateEmbeddable(int lat, int lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public int getLat() { return lat; }
    public int getLng() { return lng; }
}