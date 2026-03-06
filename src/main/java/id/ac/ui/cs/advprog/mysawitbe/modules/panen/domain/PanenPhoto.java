package id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain;

import java.util.UUID;

public class PanenPhoto {
    private final UUID id;
    private final String photoUrl;

    public PanenPhoto(UUID id, String photoUrl) {
        this.id = id;
        this.photoUrl = photoUrl;
    }

    public static PanenPhoto create(String photoUrl) {
        return new PanenPhoto(UUID.randomUUID(), photoUrl);
    }

    public UUID getId() {
        return id;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
