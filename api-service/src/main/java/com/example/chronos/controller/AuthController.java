package com.example.chronos.controller;

import com.example.chronos.domain.User;
import com.example.chronos.dto.auth.LoginRequest;
import com.example.chronos.dto.auth.LoginResponse;
import com.example.chronos.repository.UserRepository;
import com.example.chronos.security.JwtTokenUtil;
import com.example.chronos.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService; // ✅ Changed from @Autowired to final

    // ✅ Use constructor injection instead of @Autowired field
    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        System.out.println("=== REGISTER REQUEST RECEIVED ===");
        System.out.println("Username: " + user.getUsername());

        User registeredUser = authService.register(user);
        System.out.println("User registered successfully: " + registeredUser.getUsername());
        System.out.println("Assigned roles: " + registeredUser.getRoles()); // ✅ Log roles

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("username", registeredUser.getUsername());
        response.put("roles", registeredUser.getRoles()); // ✅ Include roles in response

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody User user) {
        System.out.println("=== ADMIN REGISTER REQUEST RECEIVED ===");
        System.out.println("Username: " + user.getUsername());

        // ✅ Force ADMIN role WITH ROLE_ prefix
        user.setRoles("ROLE_ADMIN");

        User registeredUser = authService.register(user);
        System.out.println("Admin user registered successfully: " + registeredUser.getUsername());
        System.out.println("Assigned roles: " + registeredUser.getRoles());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Admin user registered successfully");
        response.put("username", registeredUser.getUsername());
        response.put("roles", registeredUser.getRoles());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        System.out.println("=== LOGIN REQUEST RECEIVED ===");
        System.out.println("Username: " + request.getUsername());

        try {
            // Authenticate user
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            System.out.println("Authentication successful");
            System.out.println("User authorities: " + authenticate.getAuthorities()); // ✅ Log roles

            // Extract username
            String username = authenticate.getName();

            // Generate JWT token
            String token = jwtTokenUtil.generateToken(username);

            System.out.println("Token generated successfully");

            return ResponseEntity.ok(new LoginResponse(token));
        } catch (Exception e) {
            System.err.println("Login failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}