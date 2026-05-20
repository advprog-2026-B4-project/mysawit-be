package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
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
    PayrollPageDTO getPayrollsByUserId(UUID userId, LocalDate startDate, LocalDate endDate, String status, int page, int size);

    /**
     * Admin view: returns ALL payrolls across all users, filterable by date and status.
     */
    @PreAuthorize("hasRole('ADMIN')")
    PayrollPageDTO listAllPayrolls(LocalDate startDate, LocalDate endDate, String status, int page, int size);
}
