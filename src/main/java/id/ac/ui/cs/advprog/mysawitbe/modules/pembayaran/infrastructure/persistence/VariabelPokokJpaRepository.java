package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.persistence;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link VariabelPokokEntity}.
 */
public interface VariabelPokokJpaRepository extends JpaRepository<VariabelPokokEntity, VariableKey> {}
