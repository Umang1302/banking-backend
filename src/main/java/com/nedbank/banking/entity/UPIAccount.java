package com.nedbank.banking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to map UPI IDs to bank accounts
 * Allows users to register and use UPI IDs for payments
 */
@Entity
@Table(name = "upi_accounts", indexes = {
    @Index(name = "idx_upi_id", columnList = "upi_id"),
    @Index(name = "idx_upi_account", columnList = "account_id"),
    @Index(name = "idx_upi_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UPIAccount {

    // Status Constants
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upi_id", unique = true, nullable = false, length = 100)
    private String upiId;  // e.g., user@paytm, 9876543210@ybl

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnore
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "upi_provider", length = 50)
    private String upiProvider;  // e.g., PAYTM, PHONEPE, GOOGLEPAY, BHIM

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_ACTIVE;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void verify() {
        this.isVerified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    // Extract UPI provider from UPI ID
    public static String extractProvider(String upiId) {
        if (upiId == null || !upiId.contains("@")) {
            return "UNKNOWN";
        }
        String handle = upiId.substring(upiId.lastIndexOf("@") + 1).toLowerCase();
        return switch (handle) {
            case "paytm" -> "PAYTM";
            case "ybl" -> "PHONEPE";
            case "okaxis", "okhdfcbank", "okicici" -> "GOOGLEPAY";
            case "upi" -> "BHIM";
            default -> handle.toUpperCase();
        };
    }
}

