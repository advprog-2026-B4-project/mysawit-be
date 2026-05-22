package id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.mysawitbe.common.domain.Weight;

public class Panen {
    private final UUID panenId;
    private final UUID buruhId;
    private final String buruhName;
    private final UUID kebunId;
    private final String description;
    private final Weight weight;
    private PanenStatus status;
    private String rejectionReason;
    private final LocalDateTime timestamp;
    private final List<PanenPhoto> photos;

    public Panen(UUID panenId, UUID buruhId, String buruhName, UUID kebunId, String description, Weight weight, PanenStatus status, String rejectionReason, LocalDateTime timestamp, List<PanenPhoto> photos) {
        this.panenId = panenId;
        this.buruhId = buruhId;
        this.buruhName = buruhName;
        this.kebunId = kebunId;
        this.description = description;
        this.weight = weight;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.timestamp = timestamp;
        this.photos = photos;
    }

    public static Panen catatBaru(UUID buruhId,
        String buruhName,
        UUID kebunId, String description, Weight weight,
        LocalDateTime timestamp, List<String> photoUrls) {

            List<PanenPhoto> domainPhotos = photoUrls.stream()
            .map(PanenPhoto::create)
            .toList();

            return new Panen(UUID.randomUUID(), buruhId, buruhName, kebunId, description, weight, PanenStatus.PENDING, null, timestamp, domainPhotos);
    }

    public void approve() {
        if (this.status != PanenStatus.PENDING) {
            throw new IllegalStateException("Hanya hasil panen dengan status PENDING yang dapat disetujui.");
        }
        this.status = PanenStatus.APPROVED;
        this.rejectionReason = null; 
    }

    public void reject(String reason) {
        if (this.status != PanenStatus.PENDING) {
            throw new IllegalStateException("Hanya hasil panen dengan status PENDING yang dapat ditolak.");
        }   
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Alasan penolakan harus ada.");
        }
        this.status = PanenStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public UUID getPanenId() {
        return panenId;
    }

    public UUID getBuruhId() {
        return buruhId;
    }

    public String getBuruhName() {
        return buruhName;
    }

    public UUID getKebunId() {
        return kebunId;
    }

    public Weight getWeight() {
        return weight;
    }

    public PanenStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<PanenPhoto> getPhotos() {
        return photos;
    }

    public String getDescription() {
        return description;
    }
}