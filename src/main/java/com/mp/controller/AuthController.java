// /m_p_sharda_B-main/src/main/java/com/mp/controller/AuthController.java
package com.mp.controller;

import com.mp.dto.UserDTO;
import com.mp.entity.Role;
import com.mp.entity.User;
import com.mp.repository.UserRepository;
import com.mp.security.JwtUtil;
import com.mp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, UserRepository userRepository, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register endpoint.
     * Prevents creation of ADMIN role from client side.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        // Reject attempts to create ADMIN from client
        if (req.roles != null && req.roles.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"))) {
            Map<String, String> body = Collections.singletonMap("message", "Creating ADMIN via public register is not allowed.");
            return ResponseEntity.badRequest().body(body);
        }

        // map role strings to Role enum set (default GENERAL_USER)
        Set<Role> roles = (req.roles == null || req.roles.isEmpty())
                ? Set.of(Role.GENERAL_USER)
                : req.roles.stream()
                    .map(String::trim)
                    .filter(s -> !s.equalsIgnoreCase("ADMIN")) // extra safety
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());

        UserDTO created = userService.create(req.email, req.name, req.password, roles, req.userType, req.institutionId);
        return ResponseEntity.ok(created);
    }

    /**
     * Login endpoint. Returns token + email + roles on success.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email, req.password));
            var userOpt = userRepository.findByEmail(req.email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Collections.singletonMap("message", "User not found after authentication"));
            }
            User user = userOpt.get();
            Set<String> roleNames = user.getRoles() == null ? Set.of() : user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
            String token = jwtUtil.generateToken(user.getEmail(), roleNames);
            return ResponseEntity.ok(new LoginResponse(token, user.getEmail(), roleNames));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body(Collections.singletonMap("message", "Invalid credentials"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "Server error during authentication"));
        }
    }

    // DTOs for auth requests/responses
    public static class RegisterRequest {
        public String email;
        public String name;
        public String password;
        public Set<String> roles; // optional, e.g. ["TEACHER"]
        public String userType;
        public Long institutionId;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static record LoginResponse(String token, String email, Set<String> roles) {}
}
