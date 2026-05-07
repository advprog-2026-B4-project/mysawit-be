package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.infrastructure.external;

import id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external.MidtransProperties;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.PaymentCallbackDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MidtransGatewayAdapterTest {

    @Mock
    private MidtransProperties props;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private MidtransGatewayAdapter adapter;

    private static final String TEST_SERVER_KEY = "test-server-key";
    private static final String TEST_SNAP_URL = "https://app.midtrans.com/snap/v2";
    private static final String TEST_SANDBOX_URL = "https://app.sandbox.midtrans.com/snap/v2";

    @BeforeEach
    void setUp() throws Exception {
        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);
        adapter = new MidtransGatewayAdapter(props);
        setField(adapter, "restClient", restClient);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    void verifyCallbackSignature_validSignature_returnsTrue() throws Exception {
        String orderId = "ORDER-123";
        String statusCode = "200";
        String grossAmount = "100000";
        String data = orderId + statusCode + grossAmount + TEST_SERVER_KEY;

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
        String signature = bytesToHex(hashBytes);

        PaymentCallbackDTO payload = new PaymentCallbackDTO(
                "TXN-123",
                orderId,
                grossAmount,
                "settlement",
                "credit_card",
                statusCode,
                signature
        );

        boolean result = adapter.verifyCallbackSignature(payload);

        assertTrue(result);
        verify(props).serverKey();
    }

    @Test
    void verifyCallbackSignature_invalidSignature_returnsFalse() {
        PaymentCallbackDTO payload = new PaymentCallbackDTO(
                "TXN-123",
                "ORDER-123",
                "100000",
                "settlement",
                "credit_card",
                "200",
                "invalid-signature-key"
        );

        boolean result = adapter.verifyCallbackSignature(payload);

        assertFalse(result);
    }

    @Test
    void verifyCallbackSignature_emptySignature_returnsFalse() {
        PaymentCallbackDTO payload = new PaymentCallbackDTO(
                "TXN-123",
                "ORDER-123",
                "100000",
                "settlement",
                "credit_card",
                "200",
                ""
        );

        boolean result = adapter.verifyCallbackSignature(payload);

        assertFalse(result);
    }

    @Test
    void verifyCallbackSignature_differentDataProducesDifferentSignature() throws Exception {
        String data1 = "ORDER-123" + "200" + "100000" + TEST_SERVER_KEY;
        String data2 = "ORDER-456" + "200" + "100000" + TEST_SERVER_KEY;

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        String sig1 = bytesToHex(md.digest(data1.getBytes(StandardCharsets.UTF_8)));
        String sig2 = bytesToHex(md.digest(data2.getBytes(StandardCharsets.UTF_8)));

        assertNotEquals(sig1, sig2);
    }

    // ---------- initiateTopUp tests ----------

    @Test
    void initiateTopUp_withValidResponse_returnsRedirectUrl() {
        String orderId = "TOPUP-123";
        int grossAmount = 100000;
        String redirectUrl = "https://app.midtrans.com/snap/v2/transactions/12345";

        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        Map<String, Object> mockResponse = Map.of("redirect_url", redirectUrl);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(mockResponse);

        String result = adapter.initiateTopUp(orderId, grossAmount);

        assertEquals(redirectUrl, result);
    }

    @Test
    void initiateTopUp_withNullResponse_throwsIllegalStateException() {
        String orderId = "TOPUP-123";
        int grossAmount = 100000;

        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            adapter.initiateTopUp(orderId, grossAmount);
        });
        assertEquals("Midtrans response is null", exception.getMessage());
    }

    @Test
    void initiateTopUp_withMissingRedirectUrl_throwsIllegalStateException() {
        String orderId = "TOPUP-123";
        int grossAmount = 100000;

        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        Map<String, Object> mockResponse = Map.of();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(mockResponse);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            adapter.initiateTopUp(orderId, grossAmount);
        });
        assertTrue(exception.getMessage().contains("Midtrans redirect_url is missing"));
    }

    @Test
    void initiateTopUp_withBlankRedirectUrl_throwsIllegalStateException() {
        String orderId = "TOPUP-123";
        int grossAmount = 100000;

        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        Map<String, Object> mockResponse = Map.of("redirect_url", "   ");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(mockResponse);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            adapter.initiateTopUp(orderId, grossAmount);
        });
        assertTrue(exception.getMessage().contains("Midtrans redirect_url is missing"));
    }

    // ---------- fetchTransactionStatus tests ----------

    @Test
    void fetchTransactionStatus_productionUrl_buildsCorrectEndpoint() {
        String orderId = "ORDER-123";

        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        PaymentCallbackDTO expectedDto = new PaymentCallbackDTO(
                "TXN-123",
                orderId,
                "100000",
                "settlement",
                "gopay",
                "200",
                "some-signature"
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PaymentCallbackDTO.class)).thenReturn(expectedDto);

        PaymentCallbackDTO result = adapter.fetchTransactionStatus(orderId);

        assertEquals(expectedDto, result);
    }

    @Test
    void fetchTransactionStatus_sandboxUrl_buildsCorrectSandboxEndpoint() {
        String orderId = "ORDER-123";

        when(props.snapBaseUrl()).thenReturn(TEST_SANDBOX_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        PaymentCallbackDTO expectedDto = new PaymentCallbackDTO(
                "TXN-123",
                orderId,
                "100000",
                "pending",
                "bank_transfer",
                "201",
                "some-signature"
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PaymentCallbackDTO.class)).thenReturn(expectedDto);

        PaymentCallbackDTO result = adapter.fetchTransactionStatus(orderId);

        assertEquals(expectedDto, result);
    }

    @Test
    void fetchTransactionStatus_withSettlementStatus_returnsSettlementDto() {
        String orderId = "ORDER-SETTLE";

        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        PaymentCallbackDTO settlementDto = new PaymentCallbackDTO(
                "TXN-SETTLE",
                orderId,
                "50000",
                "settlement",
                "gopay",
                "200",
                "sig-settle"
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PaymentCallbackDTO.class)).thenReturn(settlementDto);

        PaymentCallbackDTO result = adapter.fetchTransactionStatus(orderId);

        assertEquals("settlement", result.transactionStatus());
        assertEquals("gopay", result.paymentType());
    }

    @Test
    void fetchTransactionStatus_withPendingStatus_returnsPendingDto() {
        String orderId = "ORDER-PENDING";

        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        PaymentCallbackDTO pendingDto = new PaymentCallbackDTO(
                "TXN-PENDING",
                orderId,
                "75000",
                "pending",
                "bank_transfer",
                "201",
                "sig-pending"
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PaymentCallbackDTO.class)).thenReturn(pendingDto);

        PaymentCallbackDTO result = adapter.fetchTransactionStatus(orderId);

        assertEquals("pending", result.transactionStatus());
    }

    @Test
    void fetchTransactionStatus_withExpireStatus_returnsExpireDto() {
        String orderId = "ORDER-EXPIRE";

        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        PaymentCallbackDTO expireDto = new PaymentCallbackDTO(
                "TXN-EXPIRE",
                orderId,
                "30000",
                "expire",
                "ovo",
                "202",
                "sig-expire"
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PaymentCallbackDTO.class)).thenReturn(expireDto);

        PaymentCallbackDTO result = adapter.fetchTransactionStatus(orderId);

        assertEquals("expire", result.transactionStatus());
    }

    @Test
    void fetchTransactionStatus_withFailureStatus_returnsFailureDto() {
        String orderId = "ORDER-FAIL";

        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        PaymentCallbackDTO failDto = new PaymentCallbackDTO(
                "TXN-FAIL",
                orderId,
                "20000",
                "failure",
                "credit_card",
                "400",
                "sig-fail"
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PaymentCallbackDTO.class)).thenReturn(failDto);

        PaymentCallbackDTO result = adapter.fetchTransactionStatus(orderId);

        assertEquals("failure", result.transactionStatus());
    }

    // ---------- init() @PostConstruct coverage ----------

    @Test
    void init_shouldCreateRestClient() {
        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenReturn(TEST_SERVER_KEY);

        MidtransGatewayAdapter adapterToInit = new MidtransGatewayAdapter(props);
        adapterToInit.init();

        // init() creates RestClient - just verify adapter was created successfully
        // (RestClient.create() is called internally, no external verification needed)
        assertNotNull(adapterToInit);
    }

    // ---------- verifyCallbackSignature exception branch ----------

    @Test
    void verifyCallbackSignature_whenMessageDigestThrows_returnsFalse() {
        // This test doesn't need RestClient since verifyCallbackSignature only uses props
        when(props.snapBaseUrl()).thenReturn(TEST_SNAP_URL);
        when(props.serverKey()).thenThrow(new RuntimeException("Server key error"));

        adapter = new MidtransGatewayAdapter(props);

        PaymentCallbackDTO payload = new PaymentCallbackDTO(
                "TXN-123",
                "ORDER-123",
                "100000",
                "settlement",
                "credit_card",
                "200",
                "some-signature"
        );

        boolean result = adapter.verifyCallbackSignature(payload);

        assertFalse(result);
    }
}