package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "kebun_supir",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_supir_one_kebun", columnNames = {"supir_id"})
        }
)
public class KebunSupirJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "kebun_id", nullable = false)
    private UUID kebunId;

    @Column(name = "supir_id", nullable = false)
    private UUID supirId;

    protected KebunSupirJpaEntity() {}

    public KebunSupirJpaEntity(UUID kebunId, UUID supirId) {
        this.kebunId = kebunId;
        this.supirId = supirId;
    }

    public UUID getId() { return id; }
    public UUID getKebunId() { return kebunId; }
    public UUID getSupirId() { return supirId; }

    public void setKebunId(UUID kebunId) { this.kebunId = kebunId; }
}