package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Inbound port: write operations for wage variables.
 * RBAC enforced here via @PreAuthorize; Spring Security AOP intercepts at the proxy boundary.
 */
public interface VariabelPokokCommandUseCase {

    /**
     * Updates the wage rate for the specified variable. ADMIN only.
     *
     * @param key      the variable to update (UPAH_BURUH, UPAH_SUPIR, UPAH_MANDOR).
     * @param newValue new wage rate in SawitDollar per kg; must be positive.
     * @return the updated variable as DTO.
     * @throws jakarta.persistence.EntityNotFoundException if key not seeded.
     * @throws IllegalArgumentException                   if newValue <= 0.
     */
    @PreAuthorize("hasRole('ADMIN')")
    VariabelPokokDTO updateVariabelPokok(VariableKey key, int newValue);
}
