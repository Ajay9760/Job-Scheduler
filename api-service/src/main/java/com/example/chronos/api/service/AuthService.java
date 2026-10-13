package com.example.chronos.api.service;

import com.example.chronos.api.dto.Dtos.*;
import com.example.chronos.api.security.JwtService;
import com.example.chronos.api.web.ApiException;
import com.example.chronos.domain.User;
import com.example.chronos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements ApplicationRunner {


    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private String dummyHash;

    @Value("${chronos.admin.username:}") private String adminUsername;
    @Value("${chronos.admin.password:}") private String adminPassword;

    /** Self-registration always creates ROLE_USER. There is no way to request a role over the API. */
    @Transactional
    public TokenResponse register(RegisterRequest r) {
        User u = new User();
        u.setUsername(r.username());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRoles("ROLE_USER");
        try {
            users.saveAndFlush(u);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }
        return new TokenResponse(jwt.issue(u));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest r) {
        User u = users.findByUsername(r.username()).orElse(null);
        // Always run one bcrypt comparison so response time doesn't reveal whether the user exists.
        boolean ok = encoder.matches(r.password(), u == null ? dummyHash() : u.getPasswordHash());
        if (u == null || !ok || !u.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return new TokenResponse(jwt.issue(u));
    }

    private synchronized String dummyHash() {
        if (dummyHash == null) {
            dummyHash = encoder.encode("not-a-real-password");
        }
        return dummyHash;
    }

    /** Revoke every token of the user (e.g. after a password change or suspected leak). */
    @Transactional
    public void revokeTokens(String username) {
        users.findByUsername(username).ifPresent(u -> u.setTokenVersion(u.getTokenVersion() + 1));
    }

    /** The only way to get an admin: set ADMIN_USERNAME / ADMIN_PASSWORD in the environment. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (adminPassword.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 12 characters");
        }
        if (!users.existsByUsername(adminUsername)) {
            User u = new User();
            u.setUsername(adminUsername);
            u.setPasswordHash(encoder.encode(adminPassword));
            u.setRoles("ROLE_USER,ROLE_ADMIN");
            users.save(u);
            log.info("Bootstrapped admin user '{}'", adminUsername);
        }
    }
}
