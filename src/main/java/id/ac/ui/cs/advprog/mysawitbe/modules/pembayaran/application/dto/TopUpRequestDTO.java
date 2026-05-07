package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

/**
 * Request DTO for admin wallet top-up via Midtrans Snap.
 * amount: top-up amount in IDR (integer, e.g. 100000 for Rp100.000).
 */
public record TopUpRequestDTO(
        int amount,
        String paymentMethod
) {}
