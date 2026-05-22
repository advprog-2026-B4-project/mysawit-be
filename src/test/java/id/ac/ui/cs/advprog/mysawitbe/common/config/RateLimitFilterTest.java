package id.ac.ui.cs.advprog.mysawitbe.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private LettuceBasedProxyManager<String> proxyManager;

    private StringWriter responseWriter;

    private final ConcurrentMap<String, BucketProxy> bucketStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        lenient().when(proxyManager.builder()).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            RemoteBucketBuilder<String> mockBuilder = mock(RemoteBucketBuilder.class);
            when(mockBuilder.build(any(), any(BucketConfiguration.class))).thenAnswer(a -> {
                String key = a.getArgument(0);
                BucketConfiguration cfg = a.getArgument(1);
                return bucketStore.computeIfAbsent(key, k -> {
                    long capacity = cfg.getBandwidths()[0].getCapacity();
                    var tokens = new AtomicLong(capacity);
                    BucketProxy mockBucket = mock(BucketProxy.class);
                    when(mockBucket.tryConsumeAndReturnRemaining(anyLong())).thenAnswer(ta -> {
                        long num = ta.getArgument(0);
                        long current = tokens.getAndUpdate(v -> Math.max(0, v - num));
                        if (current >= num) {
                            return ConsumptionProbe.consumed(current - num, 0L);
                        }
                        return ConsumptionProbe.rejected(current, 1_000_000_000L, 0L);
                    });
                    return mockBucket;
                });
            });
            return mockBuilder;
        });
    }

    @Nested
    class RateLimitEnabled {

        @Test
        void doFilterInternal_disabled_alwaysPassesThrough() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(false, proxyManager);
            filter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_getRequest_bypassesRateLimit() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("GET");
            filter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_putRequest_bypassesRateLimit() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("PUT");
            filter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_unmonitoredPostPath_bypassesRateLimit() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/panen");
            filter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_loginPath_firstRequest_passesThrough() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            filter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_registerPath_firstRequest_passesThrough() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/register");
            when(request.getRemoteAddr()).thenReturn("192.168.1.2");
            filter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_midtransPath_firstRequest_passesThrough() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/pembayaran/wallet/midtrans-callback");
            filter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_loginPath_rateLimitExceeded_returns429() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getRemoteAddr()).thenReturn("192.168.1.3");

            for (int i = 0; i < 10; i++) {
                filter.doFilterInternal(request, response, filterChain);
            }

            reset(filterChain, response);
            when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getRemoteAddr()).thenReturn("192.168.1.3");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            verify(response).setStatus(429);
            verify(response).setHeader(eq("Retry-After"), anyString());
        }

        @Test
        void doFilterInternal_midtransPath_globalBucketShared() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/pembayaran/wallet/midtrans-callback");
            filter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class ResponseFormat {

        @Test
        void doFilterInternal_rateLimited_returnsJsonErrorBody() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");

            for (int i = 0; i < 10; i++) {
                filter.doFilterInternal(request, response, filterChain);
            }

            reset(response);
            when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");

            filter.doFilterInternal(request, response, filterChain);

            String body = responseWriter.toString();
            assertNotNull(body);
            assertThat(body).contains("\"status\":\"error\"");
            assertThat(body).contains("Too many requests");
        }

        @Test
        void doFilterInternal_rateLimited_returnsContentTypeJson() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true, proxyManager);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getRemoteAddr()).thenReturn("10.0.0.2");

            for (int i = 0; i < 10; i++) {
                filter.doFilterInternal(request, response, filterChain);
            }

            reset(response);
            when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getRemoteAddr()).thenReturn("10.0.0.2");

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setContentType("application/json");
        }
    }
}
