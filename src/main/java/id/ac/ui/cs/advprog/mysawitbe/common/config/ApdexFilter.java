package id.ac.ui.cs.advprog.mysawitbe.common.config;

import id.ac.ui.cs.advprog.mysawitbe.common.metrics.ApdexMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Measures HTTP request response times and records them as APDEX metrics.
 *
 * <p>Positioned at {@link Ordered#HIGHEST_PRECEDENCE} to capture the full
 * end-to-end response time including all downstream filters (auth, rate-limit,
 * etc.) and controller processing.
 *
 * <p>Actuator management endpoints are excluded from measurement.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApdexFilter extends OncePerRequestFilter {

    private final ApdexMetrics apdexMetrics;

    public ApdexFilter(ApdexMetrics apdexMetrics) {
        this.apdexMetrics = apdexMetrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            apdexMetrics.logRequest(elapsedMs);
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }
}
