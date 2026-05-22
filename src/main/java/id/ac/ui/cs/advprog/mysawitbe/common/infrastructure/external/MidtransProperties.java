package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.midtrans")
public record MidtransProperties(
        String serverKey,
        String clientKey,
        String merchantId,
        String snapBaseUrl,
        String notificationUrl,
        String redirectUrl,
        @DefaultValue("5000") long connectTimeoutMs,
        @DefaultValue("10000") long readTimeoutMs
) {}
