package id.ac.ui.cs.advprog.mysawitbe.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

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

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Nested
    class RateLimitEnabled {

        @Test
        void doFilterInternal_disabled_alwaysPassesThrough() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_getRequest_bypassesRateLimit() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true);
            when(request.getMethod()).thenReturn("GET");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_putRequest_bypassesRateLimit() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true);
            when(request.getMethod()).thenReturn("PUT");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_unmonitoredPostPath_bypassesRateLimit() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/panen");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_loginPath_firstRequest_passesThrough() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_registerPath_firstRequest_passesThrough() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/register");
            when(request.getRemoteAddr()).thenReturn("192.168.1.2");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_midtransPath_firstRequest_passesThrough() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/pembayaran/wallet/midtrans-callback");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_loginPath_rateLimitExceeded_returns429() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getRemoteAddr()).thenReturn("192.168.1.3");

            // Exhaust the bucket (login: 10 requests / 15 min)
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
            verify(response).setHeader(eq("Retry-After"),
                    anyString());
        }

        @Test
        void doFilterInternal_midtransPath_globalBucketShared() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true);
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/pembayaran/wallet/midtrans-callback");

            // Midtrans: 60 req/min. First request should pass.
            filter.doFilterInternal(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class ResponseFormat {

        @Test
        void doFilterInternal_rateLimited_returnsJsonErrorBody() throws ServletException, IOException {
            RateLimitFilter filter = new RateLimitFilter(true);
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
            RateLimitFilter filter = new RateLimitFilter(true);
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
