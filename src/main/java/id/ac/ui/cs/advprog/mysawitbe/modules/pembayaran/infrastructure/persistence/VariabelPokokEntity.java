package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity mapping the {@code variables} table.
 * Lives only in the infrastructure layer - never exposed to the domain or application layers.
 */
@Entity
@Table(name = "variables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VariabelPokokEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "key", nullable = false, updatable = false)
    private VariableKey key;

    @Column(name = "value", nullable = false)
    private int value;
}
