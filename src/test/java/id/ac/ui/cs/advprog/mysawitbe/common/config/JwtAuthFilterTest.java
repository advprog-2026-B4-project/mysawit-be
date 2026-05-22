package id.ac.ui.cs.advprog.mysawitbe.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external.JwtService;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_ID_STRING = USER_ID.toString();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class DoFilterInternal {

        @Test
        void doFilterInternal_noAuthHeader_chainContinuesWithoutAuth() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).isTokenValid(any());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        void doFilterInternal_emptyAuthHeader_chainContinuesWithoutAuth() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).isTokenValid(any());
        }

        @Test
        void doFilterInternal_nonBearerAuthHeader_chainContinuesWithoutAuth() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).isTokenValid(any());
        }

        @Test
        void doFilterInternal_validBearerToken_setsAuthentication() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID_STRING);
            when(jwtService.extractRole(VALID_TOKEN)).thenReturn("BURUH");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService).isTokenValid(VALID_TOKEN);
            verify(jwtService).extractUserId(VALID_TOKEN);
            verify(jwtService).extractRole(VALID_TOKEN);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals(USER_ID_STRING, auth.getPrincipal());
            assertEquals(1, auth.getAuthorities().size());
            assertEquals("ROLE_BURUH", auth.getAuthorities().iterator().next().getAuthority());
        }

        @Test
        void doFilterInternal_validToken_setsUserIdRequestAttribute() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID_STRING);
            when(jwtService.extractRole(VALID_TOKEN)).thenReturn("MANDOR");

            ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(request).setAttribute(org.mockito.ArgumentMatchers.eq("userId"), userIdCaptor.capture());
            assertEquals(USER_ID, userIdCaptor.getValue());
        }

        @Test
        void doFilterInternal_validToken_mandorRole_setsCorrectRole() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID_STRING);
            when(jwtService.extractRole(VALID_TOKEN)).thenReturn("MANDOR");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals("ROLE_MANDOR", auth.getAuthorities().iterator().next().getAuthority());
        }

        @Test
        void doFilterInternal_validToken_adminRole_setsCorrectRole() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID_STRING);
            when(jwtService.extractRole(VALID_TOKEN)).thenReturn("ADMIN");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals("ROLE_ADMIN", auth.getAuthorities().iterator().next().getAuthority());
        }

        @Test
        void doFilterInternal_invalidToken_chainContinuesWithoutAuth() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(false);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).extractUserId(any());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        void doFilterInternal_expiredToken_chainContinuesWithoutAuth() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(false);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(jwtService).isTokenValid(VALID_TOKEN);
            verify(jwtService, never()).extractUserId(any());
        }

        @Test
        void doFilterInternal_malformedBearerHeader_chainContinuesWithoutAuth() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_clearsPreviousSecurityContext() throws ServletException, IOException {
            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.TestingAuthenticationToken(
                            "oldUser", null));

            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID_STRING);
            when(jwtService.extractRole(VALID_TOKEN)).thenReturn("BURUH");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals(USER_ID_STRING, auth.getPrincipal());
        }
    }
}
