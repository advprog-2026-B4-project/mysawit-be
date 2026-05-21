package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "pengiriman_panen_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PengirimanPanenItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pengiriman_item_id", nullable = false, updatable = false)
    private UUID pengirimanItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pengiriman_id", nullable = false)
    private PengirimanJpaEntity pengiriman;

    @Column(name = "panen_id", nullable = false, unique = true)
    private UUID panenId;
}
