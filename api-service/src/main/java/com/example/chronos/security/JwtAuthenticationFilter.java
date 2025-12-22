package com.example.chronos.security;

import com.example.chronos.domain.User;
import com.example.chronos.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserRepository userRepository) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // 1. Skip validation for public endpoints
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 2. If no Bearer header, DO NOT block – just continue the chain.
        if (header == null || !header.startsWith("Bearer ")) {
            if (!"OPTIONS".equals(request.getMethod())) {
                logger.warn("No valid Auth Header found for endpoint: {}. Proceeding without JWT auth.", requestPath);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 3. We have a Bearer token – validate it
        String token = header.substring(7);
        try {
            String username = jwtTokenUtil.extractUsername(token);
            logger.debug("Authenticating user via JWT: {}", username);

            if (username != null && jwtTokenUtil.validateToken(token)) {
                User user = userRepository.findByUsername(username).orElse(null);

                if (user != null) {
                    logger.debug("User authenticated successfully: {} with roles: {}", username, user.getRoles());

                    var authorities = Arrays.stream(user.getRoles().split(","))
                            .map(String::trim)
                            .filter(r -> !r.isEmpty())
                            .map(SimpleGrantedAuthority::new)  // ✅ FIXED: Don't add ROLE_ prefix (already in DB)
                            .collect(Collectors.toList());

                    var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    logger.warn("Token is valid, but user '{}' was not found in the database", username);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                    return;
                }
            } else {
                logger.warn("Token validation failed for user: {}", username);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        } catch (Exception e) {
            logger.error("JWT Authentication error: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") ||
                path.startsWith("/auth/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/error") ||
                path.startsWith("/h2-console/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/");
    }
}
