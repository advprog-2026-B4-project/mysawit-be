package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Use case interface for payroll read/query operations.
 */
public interface PembayaranQueryUseCase {

    PayrollStatusDTO getPayrollStatus(UUID payrollId);

    /**
     * Returns payrolls for a specific user, filterable by date and status.
     */
    List<PayrollDTO> getPayrollsByUserId(UUID userId, LocalDate startDate, LocalDate endDate, String status);

    /**
     * Admin view: returns ALL payrolls across all users, filterable by date and status.
     */
    List<PayrollDTO> listAllPayrolls(LocalDate startDate, LocalDate endDate, String status);
}
