package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.external;

import id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external.MidtransProperties;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.out.PaymentGatewayPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
                        "gross_amount", grossAmount,
                        "notification_url", props.notificationUrl(),
                        "finish_url", props.redirectUrl()
                ),
                "customer_details", Map.of(
                        "first_name", "Admin",
                        "email", ""
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + basicAuth(props.serverKey()));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(url)
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Midtrans response is null");
        }

        String token = (String) response.get("token");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Midtrans Snap token is missing from response: " + response);
        }

        return props.snapBaseUrl() + "?token=" + token;
    }

    @Override
    public boolean verifyCallbackSignature(PaymentCallbackDTO payload) {
        try {
            // Midtrans signature: SHA512(orderId + status + grossAmount) using serverKey as HMAC key
            String data = payload.orderId()
                    + payload.transactionStatus()
                    + payload.grossAmount();
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    props.serverKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = bytesToHex(hmacBytes);
            return calculatedSignature.equals(payload.signatureKey());
        } catch (Exception e) {
            log.error("Failed to verify Midtrans callback signature", e);
            return false;
        }
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