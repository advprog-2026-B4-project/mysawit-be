package id.ac.ui.cs.advprog.mysawitbe.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.SuppliedBucketBuilder;
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

    // Cached buckets by Redis key — simulates shared Redis state
    private final ConcurrentMap<String, Bucket> bucketStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        when(proxyManager.builder()).thenAnswer(inv -> {
            var configHolder = new BucketConfiguration[1];
            var keyHolder = new String[1];

            SuppliedBucketBuilder mockBuilder = org.mockito.Mockito.mock(SuppliedBucketBuilder.class);
            when(mockBuilder.withKey(any())).thenAnswer(a -> { keyHolder[0] = (String) a.getArgument(0); return mockBuilder; });
            doAnswer(a -> { configHolder[0] = a.getArgument(0); return mockBuilder; }).when(mockBuilder).withConfiguration(any(BucketConfiguration.class));
            when(mockBuilder.build()).thenAnswer(a -> {
                String key = keyHolder[0] != null ? keyHolder[0] : "default";
                return bucketStore.computeIfAbsent(key, k -> {
                    var cfg = configHolder[0];
                    if (cfg == null) cfg = BucketConfiguration.builder()
                            .addLimit(Bandwidth.builder().capacity(100).refillGreedy(100, Duration.ofMinutes(1)).build())
                            .build();
                    return Bucket.builder().withConfiguration(cfg).build();
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
