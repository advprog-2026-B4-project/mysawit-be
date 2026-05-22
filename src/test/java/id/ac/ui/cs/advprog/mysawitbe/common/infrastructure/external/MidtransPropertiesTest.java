package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MidtransPropertiesTest {

    private final MidtransProperties properties = new MidtransProperties(
            "sk-test-123",
            "ck-test-456",
            "MERCH001",
            "https://app.sandbox.midtrans.com/snap/v1",
            "https://mysawit.com/api/payment/callback",
            "https://mysawit.com/payment/status",
            5000L,
            10000L
    );

    @Test
    void midtransProperties_bindsAllFields() {
        assertThat(properties.serverKey()).isEqualTo("sk-test-123");
        assertThat(properties.clientKey()).isEqualTo("ck-test-456");
        assertThat(properties.merchantId()).isEqualTo("MERCH001");
        assertThat(properties.snapBaseUrl()).isEqualTo("https://app.sandbox.midtrans.com/snap/v1");
        assertThat(properties.notificationUrl()).isEqualTo("https://mysawit.com/api/payment/callback");
        assertThat(properties.redirectUrl()).isEqualTo("https://mysawit.com/payment/status");
    }

    @Test
    void midtransProperties_defaultTimeoutValues() {
        assertThat(properties.connectTimeoutMs()).isEqualTo(5000);
        assertThat(properties.readTimeoutMs()).isEqualTo(10000);
    }

    @Test
    void midtransProperties_isNotNull() {
        assertThat(properties).isNotNull();
    }
}
