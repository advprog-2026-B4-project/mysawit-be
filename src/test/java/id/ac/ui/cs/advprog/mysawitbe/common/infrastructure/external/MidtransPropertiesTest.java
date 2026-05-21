package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = MidtransPropertiesTest.TestConfig.class,
        properties = {
                "spring.autoconfigure.exclude=" +
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
@TestPropertySource(properties = {
        "app.midtrans.server-key=sk-test-123",
        "app.midtrans.client-key=ck-test-456",
        "app.midtrans.merchant-id=MERCH001",
        "app.midtrans.snap-base-url=https://app.sandbox.midtrans.com/snap/v1",
        "app.midtrans.notification-url=https://mysawit.com/api/payment/callback",
        "app.midtrans.redirect-url=https://mysawit.com/payment/status",
        "app.midtrans.connect-timeout-ms=5000",
        "app.midtrans.read-timeout-ms=10000"
})
class MidtransPropertiesTest {

    @EnableConfigurationProperties(MidtransProperties.class)
    static class TestConfig {}

    @Autowired
    private MidtransProperties properties;

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
    void midtransProperties_springContextLoads() {
        assertThat(properties).isNotNull();
    }
}
