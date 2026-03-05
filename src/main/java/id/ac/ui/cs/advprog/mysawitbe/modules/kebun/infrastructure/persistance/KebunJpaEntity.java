package id.ac.ui.cs.advprog.mysawitbe.modules.kebun.infrastructure.persistance;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "kebun",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_kebun_kode", columnNames = {"kode"}),
                @UniqueConstraint(name = "uk_kebun_mandor", columnNames = {"mandor_id"})
        }
)
public class KebunJpaEntity {

    @Id
    @Column(name = "kebun_id", nullable = false)
    private UUID kebunId;

    @Column(name = "nama", nullable = false)
    private String nama;

    @Column(name = "kode", nullable = false)
    private String kode;

    @Column(name = "luas", nullable = false)
    private int luas;

    @Column(name = "mandor_id")
    private UUID mandorId;

    @ElementCollection
    @CollectionTable(
            name = "kebun_coordinate",
            joinColumns = @JoinColumn(name = "kebun_id", referencedColumnName = "kebun_id")
    )
    @OrderColumn(name = "idx")
    private List<CoordinateEmbeddable> coordinates = new ArrayList<>();

    protected KebunJpaEntity() {}

    public KebunJpaEntity(UUID kebunId, String nama, String kode, int luas, UUID mandorId, List<CoordinateEmbeddable> coordinates) {
        this.kebunId = kebunId;
        this.nama = nama;
        this.kode = kode;
        this.luas = luas;
        this.mandorId = mandorId;
        this.coordinates = coordinates;
    }

    public UUID getKebunId() { return kebunId; }
    public String getNama() { return nama; }
    public String getKode() { return kode; }
    public int getLuas() { return luas; }
    public UUID getMandorId() { return mandorId; }
    public List<CoordinateEmbeddable> getCoordinates() { return coordinates; }

    public void setNama(String nama) { this.nama = nama; }
    public void setLuas(int luas) { this.luas = luas; }
    public void setMandorId(UUID mandorId) { this.mandorId = mandorId; }
    public void setCoordinates(List<CoordinateEmbeddable> coordinates) { this.coordinates = coordinates; }
}