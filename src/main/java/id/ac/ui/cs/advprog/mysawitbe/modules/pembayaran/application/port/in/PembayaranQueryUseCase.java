package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port for payroll read/query operations.
 * RBAC enforced via @PreAuthorize; the JwtAuthFilter sets authentication.name = userId (String).
 */
public interface PembayaranQueryUseCase {

    /**
     * Returns the status of a single payroll.
     * Any authenticated user may check a payroll ID (ownership enforced in service).
     */
    @PreAuthorize("isAuthenticated()")
    PayrollStatusDTO getPayrollStatus(UUID payrollId);

    /**
     * Returns payrolls for a specific user, filterable by date and status.
     * A user may only query their own payrolls; ADMIN may query any.
     */
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    List<PayrollDTO> getPayrollsByUserId(UUID userId, LocalDate startDate, LocalDate endDate, String status);

    /**
     * Admin view: returns ALL payrolls across all users, filterable by date and status.
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<PayrollDTO> listAllPayrolls(LocalDate startDate, LocalDate endDate, String status);
}
