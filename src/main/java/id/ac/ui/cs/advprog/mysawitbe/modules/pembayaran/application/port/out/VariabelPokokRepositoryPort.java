package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for wage-variable persistence.
 * Implemented by infrastructure/persistence/adapter/VariabelPokokRepositoryAdapter.
 */
public interface VariabelPokokRepositoryPort {

    List<VariabelPokokDTO> findAll();

    Optional<VariabelPokokDTO> findByKey(VariableKey key);

    /** Persists the new value and returns the updated record. */
    VariabelPokokDTO save(VariableKey key, int newValue);
}
