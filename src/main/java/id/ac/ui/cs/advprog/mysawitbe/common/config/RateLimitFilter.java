package id.ac.ui.cs.advprog.mysawitbe.common.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH    = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String MIDTRANS_PATH = "/api/pembayaran/wallet/midtrans-callback";

    private static final String KEY_LOGIN    = "rl:login:";
    private static final String KEY_REGISTER = "rl:register:";
    private static final String KEY_MIDTRANS = "rl:midtrans:global";

    private final boolean rateLimitEnabled;
    private final LettuceBasedProxyManager<String> proxyManager;

    private final BucketConfiguration loginConfig;
    private final BucketConfiguration registerConfig;
    private final BucketConfiguration midtransConfig;

    public RateLimitFilter(
            @Value("${app.rate-limit.enabled:true}") boolean rateLimitEnabled,
            LettuceBasedProxyManager<String> proxyManager) {
        this.rateLimitEnabled = rateLimitEnabled;
        this.proxyManager = proxyManager;

        this.loginConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(15)).build())
                .build();
        this.registerConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofHours(1)).build())
                .build();
        this.midtransConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build())
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        String path   = request.getRequestURI();
        String method = request.getMethod();

        if (!"POST".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = switch (path) {
            case LOGIN_PATH    -> proxyManager.builder()
                    .build(KEY_LOGIN + clientIp(request), loginConfig);
            case REGISTER_PATH -> proxyManager.builder()
                    .build(KEY_REGISTER + clientIp(request), registerConfig);
            case MIDTRANS_PATH -> proxyManager.builder()
                    .build(KEY_MIDTRANS, midtransConfig);
            default            -> null;
        };

        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }

        var probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"status\":\"error\",\"message\":\"Too many requests. Please try again later.\"}");
        }
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
