package id.ac.ui.cs.advprog.mysawitbe.common.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
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
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH    = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String MIDTRANS_PATH = "/api/pembayaran/wallet/midtrans-callback";

    private final boolean rateLimitEnabled;

    // Per-IP buckets for user-facing auth endpoints
    private final ConcurrentHashMap<String, Bucket> loginBuckets    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    // Shared global bucket for the Midtrans webhook (server-to-server, not per-IP)
    private final Bucket midtransBucket = buildBucket(60, Duration.ofMinutes(1));

    public RateLimitFilter(@Value("${app.rate-limit.enabled:true}") boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
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
            case LOGIN_PATH    -> loginBuckets.computeIfAbsent(
                                      clientIp(request), k -> buildBucket(10, Duration.ofMinutes(15)));
            case REGISTER_PATH -> registerBuckets.computeIfAbsent(
                                      clientIp(request), k -> buildBucket(5, Duration.ofHours(1)));
            case MIDTRANS_PATH -> midtransBucket;
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
        // ForwardedHeaderFilter (registered at HIGHEST_PRECEDENCE) has already rewritten
        // getRemoteAddr() to the real client IP from X-Forwarded-For.
        return request.getRemoteAddr();
    }

    private static Bucket buildBucket(int capacity, Duration period) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, period).build())
                .build();
    }
}
