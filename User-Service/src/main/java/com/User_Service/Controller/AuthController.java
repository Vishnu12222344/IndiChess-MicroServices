package com.User_Service.Controller;

import com.User_Service.Model.User;
import com.User_Service.Repository.UserRepository;
import com.User_Service.Security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller for registration and login.
 *
 * UPDATED: Removed @CrossOrigin annotation - let SecurityConfig handle CORS
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public AuthController(AuthenticationManager authManager, JwtUtil jwtUtil,
                          UserRepository repository, PasswordEncoder encoder) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.repository = repository;
        this.encoder = encoder;
    }

    /**
     * ✅ REGISTER ENDPOINT
     * Creates a new user account
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // ✅ Validate input
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Email is required")
                );
            }

            if (user.getPassword() == null || user.getPassword().length() < 8) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Password must be at least 8 characters")
                );
            }

            if (user.getName() == null || user.getName().isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Name is required")
                );
            }

            // ✅ Check if email already exists
            if (repository.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        Map.of("error", "Email already taken")
                );
            }

            // ✅ Encode password and save user
            user.setPassword(encoder.encode(user.getPassword()));
            User savedUser = repository.save(user);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "User registered successfully",
                            "email", savedUser.getEmail(),
                            "name", savedUser.getName()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Registration failed: " + e.getMessage())
            );
        }
    }

    /**
     * ✅ LOGIN ENDPOINT
     * Authenticates user and returns JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest, HttpServletResponse response) {
        try {
            // ✅ Validate input
            if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Email and password are required")
                );
            }

            // ✅ Authenticate user
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // ✅ Generate JWT token
            String token = jwtUtil.generateToken(loginRequest.getEmail());

            // ✅ Set HttpOnly cookie for additional security
            Cookie cookie = new Cookie("token", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Set to true in production with HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 24 hours
            response.addCookie(cookie);

            // ✅ Return token in response body as well
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "email", loginRequest.getEmail(),
                    "message", "Login successful"
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Invalid email or password")
            );
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Account is disabled")
            );
        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Account is locked")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Login failed: " + e.getMessage())
            );
        }
    }
}