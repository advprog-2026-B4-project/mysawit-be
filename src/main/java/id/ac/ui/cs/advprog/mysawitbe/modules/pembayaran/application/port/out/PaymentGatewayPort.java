package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;

/**
 * Outbound port for external payment gateway integration (Midtrans).
 * Implemented by infrastructure/external/MidtransGatewayAdapter.
 */
public interface PaymentGatewayPort {

    /**
     * Initiate a top-up charge for the admin wallet.
     * Returns the payment URL or snap token from Midtrans.
     * grossAmount: total amount in IDR smallest unit.
     */
    String initiateTopUp(String orderId, int grossAmount);

    /**
     * Verify the HMAC-SHA512 signature of an incoming callback payload.
     * Returns true if the signature is valid.
     */
    boolean verifyCallbackSignature(PaymentCallbackDTO payload);
}
