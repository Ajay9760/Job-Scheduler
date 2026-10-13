package com.example.chronos.api.security;

import com.example.chronos.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (h != null && h.startsWith("Bearer ")) {
            jwt.parse(h.substring(7)).ifPresent(c -> users.findByUsername(c.getSubject())
                    .filter(u -> u.isEnabled() && Integer.valueOf(u.getTokenVersion()).equals(c.get("tv", Integer.class)))
                    .ifPresent(u -> {
                        var auth = new UsernamePasswordAuthenticationToken(u.getUsername(), null,
                                Arrays.stream(u.getRoles().split(",")).map(String::trim)
                                        .filter(s -> !s.isEmpty()).map(SimpleGrantedAuthority::new).toList());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }));
        }
        chain.doFilter(req, res);
    }
}
