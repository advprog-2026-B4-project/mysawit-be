package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import java.util.List;

/**
 * Paged payroll query result.
 */
public record PayrollPageDTO(
        List<PayrollDTO> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
