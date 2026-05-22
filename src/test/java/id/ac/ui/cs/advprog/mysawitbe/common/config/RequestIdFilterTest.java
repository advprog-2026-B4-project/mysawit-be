package id.ac.ui.cs.advprog.mysawitbe.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class RequestIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RequestIdFilter requestIdFilter;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    class DoFilterInternal {

        @Test
        void doFilterInternal_requestHasXRequestId_usesExistingId() throws ServletException, IOException {
            String existingId = "abc-123-custom-id";
            when(request.getHeader("X-Request-Id")).thenReturn(existingId);

            requestIdFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response).setHeader("X-Request-Id", existingId);
        }

        @Test
        void doFilterInternal_noXRequestId_generatesNewUuid() throws ServletException, IOException {
            when(request.getHeader("X-Request-Id")).thenReturn(null);

            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

            requestIdFilter.doFilterInternal(request, response, filterChain);

            verify(response).setHeader(eq("X-Request-Id"), headerCaptor.capture());
            String generatedId = headerCaptor.getValue();
            assertNotNull(generatedId);
            assertTrue(generatedId.matches("[0-9a-f-]{36}"),
                    "Generated ID should be a UUID, got: " + generatedId);
        }

        @Test
        void doFilterInternal_blankXRequestId_generatesNewUuid() throws ServletException, IOException {
            when(request.getHeader("X-Request-Id")).thenReturn("");

            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

            requestIdFilter.doFilterInternal(request, response, filterChain);

            verify(response).setHeader(eq("X-Request-Id"), headerCaptor.capture());
            String generatedId = headerCaptor.getValue();
            assertTrue(generatedId.matches("[0-9a-f-]{36}"));
        }

        @Test
        void doFilterInternal_whitespaceOnlyXRequestId_generatesNewUuid() throws ServletException, IOException {
            when(request.getHeader("X-Request-Id")).thenReturn("   ");

            requestIdFilter.doFilterInternal(request, response, filterChain);

            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
            verify(response).setHeader(eq("X-Request-Id"), headerCaptor.capture());
            assertTrue(headerCaptor.getValue().matches("[0-9a-f-]{36}"));
        }

        @Test
        void doFilterInternal_clearsMDCAfterFilterChain() throws ServletException, IOException {
            when(request.getHeader("X-Request-Id")).thenReturn("test-id");

            requestIdFilter.doFilterInternal(request, response, filterChain);

            assertEquals(null, MDC.get("requestId"),
                    "MDC requestId should be cleared after filter chain completes");
        }

        @Test
        void doFilterInternal_clearsMDCEvenOnException() throws ServletException, IOException {
            when(request.getHeader("X-Request-Id")).thenReturn("test-id");
            org.mockito.Mockito.doThrow(new ServletException("Test exception"))
                    .when(filterChain).doFilter(request, response);

            try {
                requestIdFilter.doFilterInternal(request, response, filterChain);
            } catch (ServletException ignored) {
                // Expected
            }

            assertEquals(null, MDC.get("requestId"),
                    "MDC requestId should be cleared even when filter chain throws");
        }

        @Test
        void doFilterInternal_setsResponseHeader() throws ServletException, IOException {
            when(request.getHeader("X-Request-Id")).thenReturn("incoming-req-id");

            requestIdFilter.doFilterInternal(request, response, filterChain);

            verify(response).setHeader("X-Request-Id", "incoming-req-id");
        }

        @Test
        void doFilterInternal_alwaysCallsFilterChain() throws ServletException, IOException {
            when(request.getHeader("X-Request-Id")).thenReturn(null);

            requestIdFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}
