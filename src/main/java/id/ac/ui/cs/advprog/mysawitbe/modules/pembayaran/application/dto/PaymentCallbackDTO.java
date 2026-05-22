package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable DTO for payment gateway callback payload (Midtrans).
 * grossAmount: in smallest currency unit (integer representation).
 * transactionStatus: e.g. settlement, pending, deny, cancel, expire.
 * signatureKey: HMAC-SHA512 signature from gateway; must be verified before processing.
 */
public record PaymentCallbackDTO(
        @JsonProperty("transaction_id")
        String transactionId,
        
        @JsonProperty("order_id")
        String orderId,
        
        @JsonProperty("gross_amount")
        String grossAmount, // Midtrans sends as string e.g., "100000.00"
        
        @JsonProperty("transaction_status")
        String transactionStatus,
        
        @JsonProperty("payment_type")
        String paymentType,
        
        @JsonProperty("status_code")
        String statusCode,
        
        @JsonProperty("signature_key")
        String signatureKey
) {}
