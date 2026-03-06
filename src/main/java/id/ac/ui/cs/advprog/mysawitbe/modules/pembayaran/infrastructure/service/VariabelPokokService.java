package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.VariabelPokokCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in.VariabelPokokQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.VariabelPokokRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariabelPokok;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates business logic for wage variables.
 * Depends on the outbound port abstraction - never on the JPA adapter directly.
 */
@Service
@RequiredArgsConstructor
public class VariabelPokokService implements VariabelPokokQueryUseCase, VariabelPokokCommandUseCase {

    private final VariabelPokokRepositoryPort repository;

    // -------------------------------------------------------------------------
    // Query operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<VariabelPokokDTO> getAllVariabelPokok() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public VariabelPokokDTO getVariabelPokok(VariableKey key) {
        return repository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Variabel pokok not found: " + key));
    }

    // -------------------------------------------------------------------------
    // Command operations (ADMIN only - enforced via @PreAuthorize on VariabelPokokCommandUseCase)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public VariabelPokokDTO updateVariabelPokok(VariableKey key, int newValue) {
        // Validate via domain invariant before persisting
        VariabelPokokDTO current = getVariabelPokok(key);
        VariabelPokok domain = new VariabelPokok(current.key(), current.value());
        domain.updateValue(newValue); // throws IllegalArgumentException if invalid

        return repository.save(key, domain.getValue());
    }
}
