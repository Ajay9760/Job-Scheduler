package com.example.chronos.security;

import com.example.chronos.domain.User;
import com.example.chronos.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        // Skip logs for health checks to keep console clean
        if (!requestPath.equals("/actuator/health")) {
            System.out.println(">>> Filtering Request: " + request.getMethod() + " " + requestPath);
        }

        // 1. Skip validation for public endpoints
        if (requestPath.startsWith("/api/auth/") ||
                requestPath.startsWith("/actuator/") ||
                requestPath.startsWith("/error") ||
                requestPath.startsWith("/h2-console/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 2. Check if Header exists
        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("❌ No valid Auth Header found. Header value: " + header);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            String username = jwtTokenUtil.extractUsername(token);
            System.out.println("🔍 Extracted Username from Token: " + username);

            if (username != null && jwtTokenUtil.validateToken(token)) {
                // 3. Check if user exists in DB
                User user = userRepository.findByUsername(username).orElse(null);

                if (user != null) {
                    System.out.println("✅ User found in DB. Roles: " + user.getRoles());

                    var authorities = Arrays.stream(user.getRoles().split(","))
                            .map(String::trim)
                            .filter(r -> !r.isEmpty())
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .collect(Collectors.toList());

                    var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("🔓 SecurityContext set successfully for: " + username);
                } else {
                    System.out.println("⛔ Token is valid, but User '" + username + "' was NOT found in the Database! (Did you reset the DB?)");
                }
            } else {
                System.out.println("⚠️ Token validation failed (Expired or Invalid signature)");
            }
        } catch (Exception e) {
            System.err.println("💥 JWT Authentication error: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}