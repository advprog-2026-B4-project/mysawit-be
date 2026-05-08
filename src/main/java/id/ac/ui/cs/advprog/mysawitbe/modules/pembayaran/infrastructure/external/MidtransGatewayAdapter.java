package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.external;

import id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external.MidtransProperties;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.PaymentGatewayPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MidtransGatewayAdapter implements PaymentGatewayPort {

    private final MidtransProperties props;
    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.create();
    }

    @Override
    public String initiateTopUp(String orderId, int grossAmount) {
        String url = props.snapBaseUrl() + "/transactions";

        Map<String, Object> body = Map.of(
                "transaction_details", Map.of(
                        "order_id", orderId,
                        "gross_amount", grossAmount
                ),
                "credit_card", Map.of(
                        "secure", true
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth(props.serverKey()))
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Midtrans response is null");
        }

        // Use the redirect_url returned by Midtrans Snap API
        String redirectUrl = (String) response.get("redirect_url");
        if (redirectUrl == null || redirectUrl.isBlank()) {
            throw new IllegalStateException("Midtrans redirect_url is missing from response: " + response);
        }
        return redirectUrl;
    }

    @Override
    public boolean verifyCallbackSignature(PaymentCallbackDTO payload) {
        try {
            // Midtrans signature: SHA512(order_id + status_code + gross_amount + server_key)
            String data = payload.orderId()
                    + payload.statusCode()
                    + payload.grossAmount()
                    + props.serverKey();
            
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = bytesToHex(hashBytes);
            
            return calculatedSignature.equals(payload.signatureKey());
        } catch (Exception e) {
            log.error("Failed to verify Midtrans callback signature", e);
            return false;
        }
    }

    @Override
    public PaymentCallbackDTO fetchTransactionStatus(String orderId) {
        String apiUrl = props.snapBaseUrl().contains("sandbox")
                ? "https://api.sandbox.midtrans.com/v2/" + orderId + "/status"
                : "https://api.midtrans.com/v2/" + orderId + "/status";

        return restClient.get()
                .uri(apiUrl)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth(props.serverKey()))
                .retrieve()
                .body(PaymentCallbackDTO.class);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private String basicAuth(String serverKey) {
        String credentials = serverKey + ":";
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}