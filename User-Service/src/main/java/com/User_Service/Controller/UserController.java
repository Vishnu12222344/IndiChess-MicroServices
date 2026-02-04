package com.User_Service.Controller;

import com.User_Service.Model.User;
import com.User_Service.Repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Exposes user-profile read endpoints.
 * Registration is handled by AuthController.
 *
 * Match Service calls GET /user/by-email/{email} internally
 * (routed through the API Gateway) to resolve a player email
 * into a lightweight user record.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    /** Health-check / smoke test */
    @GetMapping("/hello")
    public String helloWorld() {
        return "Hello from User Service";
    }

    /**
     * Match Service uses this to verify a user exists before
     * storing the email in a Match entity.
     */
    @GetMapping("/by-email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return repository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Current authenticated user's profile */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return repository.findByEmail(principal.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}