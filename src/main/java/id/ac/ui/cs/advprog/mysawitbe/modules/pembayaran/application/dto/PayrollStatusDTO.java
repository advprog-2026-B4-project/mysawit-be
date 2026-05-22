package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight payroll status summary DTO.
 * amount: in smallest currency unit.
 * status: e.g. PENDING, APPROVED, REJECTED.
 * paymentReference: external payment gateway reference ID.
 */
public record PayrollStatusDTO(
        UUID payrollId,
        UUID userId,
        int amount,
        String status,
        LocalDateTime processedAt,
        String paymentReference
) {}
