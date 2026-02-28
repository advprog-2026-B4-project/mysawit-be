package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

/**
 * Immutable DTO for payment gateway callback payload (Midtrans).
 * grossAmount: in smallest currency unit (integer representation).
 * transactionStatus: e.g. settlement, pending, deny, cancel, expire.
 * signatureKey: HMAC-SHA512 signature from gateway; must be verified before processing.
 */
public record PaymentCallbackDTO(
        String transactionId,
        String orderId,
        int grossAmount,
        String transactionStatus,
        String paymentType,
        String signatureKey
) {}
