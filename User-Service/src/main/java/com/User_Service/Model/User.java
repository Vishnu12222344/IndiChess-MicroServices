package com.User_Service.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

/**
 * User entity representing a user in the system.
 *
 * UPDATED: Removed @Size validation on password field to allow
 * OAuth users with placeholder passwords.
 */
@Data
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique=true)
    private String name;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    // Removed @Size(min = 8) validation - handled in controller
    // This allows OAuth users with placeholder passwords
    @Column(nullable = false)
    private String password;
}