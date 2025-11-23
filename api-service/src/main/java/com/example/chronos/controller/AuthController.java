package com.example.chronos.controller;

import com.example.chronos.domain.User;
import com.example.chronos.dto.auth.LoginRequest;
import com.example.chronos.dto.auth.LoginResponse;
import com.example.chronos.repository.UserRepository;
import com.example.chronos.security.JwtTokenUtil;
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

@RestController
@RequestMapping("/api/auth")  // ✅ FIXED: Added "/api" prefix
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody LoginRequest request) {
        System.out.println("=== REGISTER REQUEST RECEIVED ===");
        System.out.println("Username: " + request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            System.out.println("User already exists");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username exists");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRoles("USER");  // Default role

        try {
            userRepository.save(newUser);
            System.out.println("User registered successfully: " + request.getUsername());
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            System.err.println("Error saving user: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed");
        }
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
