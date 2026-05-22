package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pengiriman")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PengirimanJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pengiriman_id", nullable = false, updatable = false)
    private UUID pengirimanId;

    @Column(name = "supir_id", nullable = false)
    private UUID supirId;

    @Column(name = "mandor_id", nullable = false)
    private UUID mandorId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "total_weight", nullable = false)
    private int totalWeight;

    @Column(name = "accepted_weight", nullable = false)
    private int acceptedWeight;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @OneToMany(mappedBy = "pengiriman", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PengirimanPanenItemJpaEntity> panenItems = new ArrayList<>();

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
