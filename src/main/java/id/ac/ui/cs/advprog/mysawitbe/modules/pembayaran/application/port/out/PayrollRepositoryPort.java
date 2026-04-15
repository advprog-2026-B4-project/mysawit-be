package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollPageDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PayrollStatusDTO;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Outbound port for payroll persistence.
 * Implemented by infrastructure/persistence/PayrollJpaAdapter.
 */
public interface PayrollRepositoryPort {

    PayrollDTO save(PayrollDTO payrollDTO);

    PayrollDTO findById(UUID payrollId);

    PayrollStatusDTO findStatusById(UUID payrollId);

    PayrollPageDTO findByUserId(UUID userId, LocalDate startDate, LocalDate endDate, String status, int page, int size);

    PayrollPageDTO findAll(LocalDate startDate, LocalDate endDate, String status, int page, int size);

    /**
     * Returns the currently configured wage rate for a given role.
     * role: BURUH, SUPIR, or MANDOR.
     */
    int getWageRate(String role);

    void updateWageRate(String role, int newRatePerGram);
}
