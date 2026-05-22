package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

/**
 * Response DTO for admin wallet top-up.
 * paymentUrl: Midtrans Snap payment page URL for the user to complete payment.
 */
public record TopUpResponseDTO(
        String paymentUrl
) {}
