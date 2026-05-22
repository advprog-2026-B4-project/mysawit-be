package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for rejecting a payroll entry.
 */
public record RejectPayrollRequest(
	@NotBlank(message = "reason is required")
	String reason
) {}
