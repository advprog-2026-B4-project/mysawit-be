package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.midtrans")
public record MidtransProperties(
        String serverKey,
        String clientKey,
        String merchantId,
        String snapBaseUrl,
        String notificationUrl,
        String redirectUrl
) {}