package id.ac.ui.cs.advprog.mysawitbe.common.config;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.external.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_ROLE = "role";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtService.isTokenValid(token)) {
                String userId = jwtService.extractUserId(token);
                String role   = jwtService.extractRole(token);

                MDC.put(ATTR_USER_ID, userId);
                MDC.put(ATTR_ROLE, role);

                request.setAttribute(ATTR_USER_ID, UUID.fromString(userId));

                var auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(ATTR_USER_ID);
            MDC.remove(ATTR_ROLE);
        }
    }
}