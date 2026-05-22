package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Immutable data transfer object for payroll calculation details.
 * referenceType: "PANEN" or "PENGIRIMAN".
 * weight: in grams.
 * wageRateApplied: wage per gram in smallest currency unit.
 * netAmount: in smallest currency unit (e.g. IDR cents or IDR directly).
 * status: e.g. PENDING, APPROVED, REJECTED.
 */
public record PayrollDTO(
        UUID payrollId,
        UUID userId,
        String role,
        UUID referenceId,
        String referenceType,
        int weight,
        int wageRateApplied,
        int netAmount,
        String status,
        String rejectionReason,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        List<String> evidencePhotoUrls
) {
        public PayrollDTO {
                evidencePhotoUrls = evidencePhotoUrls == null ? List.of() : List.copyOf(evidencePhotoUrls);
        }

        public PayrollDTO(
                UUID payrollId,
                UUID userId,
                String role,
                UUID referenceId,
                String referenceType,
                int weight,
                int wageRateApplied,
                int netAmount,
                String status,
                String rejectionReason,
                LocalDateTime processedAt,
                LocalDateTime createdAt
        ) {
                this(
                        payrollId,
                        userId,
                        role,
                        referenceId,
                        referenceType,
                        weight,
                        wageRateApplied,
                        netAmount,
                        status,
                        rejectionReason,
                        processedAt,
                        createdAt,
                        List.of()
                );
        }

}
