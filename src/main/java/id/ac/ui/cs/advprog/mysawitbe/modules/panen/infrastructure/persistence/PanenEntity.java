package id.ac.ui.cs.advprog.mysawitbe.modules.panen.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain.PanenStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "harvest_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanenEntity {

    @Id
    @Column(name = "id")
    private UUID panenId;

    @Column(name = "buruh_id", nullable = false)
    private UUID buruhId;

    @Column(name = "kebun_id", nullable = false)
    private UUID kebunId;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PanenStatus status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Builder.Default
    @OneToMany(mappedBy = "harvestReport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PanenPhotoEntity> photos = new ArrayList<>();

    public void addPhoto(PanenPhotoEntity photo) {
        photos.add(photo);
        photo.setHarvestReport(this);
    }

    public void removePhoto(PanenPhotoEntity photo) {
        photos.remove(photo);
        photo.setHarvestReport(null);
    }
}
