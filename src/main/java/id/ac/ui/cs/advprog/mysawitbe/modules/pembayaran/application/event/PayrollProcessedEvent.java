package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.event;

import java.util.UUID;

/**
 * Published when admin approves or rejects a payroll entry.
 * status: APPROVED, REJECTED.
 * Consumed by: notification module.
 */
public record PayrollProcessedEvent(
        UUID payrollId,
        UUID userId,
        String status
) {}
