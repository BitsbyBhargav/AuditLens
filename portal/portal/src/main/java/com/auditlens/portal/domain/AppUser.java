package com.auditlens.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "Users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer id;

    @Column(name = "Name", nullable = false, length = 100)
    private String name;

    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false, length = 30)
    private Role role;

    /** BCrypt hash. Null means the account cannot sign in to the portal. */
    @Column(name = "PasswordHash", length = 100)
    private String passwordHash;

    @Column(name = "CreatedAt")
    private Instant createdAt = Instant.now();

    protected AppUser() {}

    public AppUser(String name, String email, Role role, String passwordHash) {
        this.name = name; this.email = email; this.role = role; this.passwordHash = passwordHash;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
