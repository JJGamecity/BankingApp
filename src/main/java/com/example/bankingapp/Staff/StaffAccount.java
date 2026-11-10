package com.example.bankingapp.Staff;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "staff_accounts")
@Getter
public class StaffAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffRole role;

    // Nullable for staff created before creation attribution was introduced.
    @Column(updatable = false)
    private String createdBy;

    @Column(updatable = false)
    private java.time.Instant createdAt;

    public void recordCreator(String username) {
        if (createdAt != null) throw new IllegalStateException("Creator is already recorded");
        createdBy = username;
        createdAt = java.time.Instant.now();
    }

    protected StaffAccount() { }

    public StaffAccount(String username, String encodedPassword, StaffRole role) {
        this.username = username;
        this.password = encodedPassword;
        this.role = role;
    }
}
