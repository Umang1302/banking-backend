package com.nedbank.banking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to store QR Payment Request details
 * When a user generates a QR code for receiving payment
 */
@Entity
@Table(name = "qr_payment_requests", indexes = {
    @Index(name = "idx_qr_request_id", columnList = "request_id"),
    @Index(name = "idx_qr_status", columnList = "status"),
    @Index(name = "idx_qr_receiver", columnList = "receiver_account_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QRPaymentRequest {

    // Status Constants
    public static final String STATUS_CREATED = "CREATED";       // QR generated
    public static final String STATUS_PAID = "PAID";            // Payment completed
    public static final String STATUS_EXPIRED = "EXPIRED";       // QR expired
    public static final String STATUS_FAILED = "FAILED";        // Payment failed
    public static final String STATUS_CANCELLED = "CANCELLED";   // Cancelled by user

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", unique = true, nullable = false, length = 50)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_account_id", nullable = false)
    @JsonIgnore
    private Account receiverAccount;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_CREATED;

    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;  // Base64 encoded QR image

    @Column(name = "qr_type", length = 20)
    @Builder.Default
    private String qrType = "DYNAMIC";  // DYNAMIC or STATIC

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "paid_by", length = 100)
    private String paidBy;  // Username who paid

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_account_id")
    @JsonIgnore
    private Account payerAccount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

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
        if (requestId == null) {
            requestId = generateRequestId();
        }
        // Default expiry: 24 hours from creation
        if (expiresAt == null) {
            expiresAt = now.plusHours(24);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateRequestId() {
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 10000);
        return "QR" + timestamp + String.format("%04d", random);
    }

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPaid() {
        return STATUS_PAID.equals(status);
    }

    public boolean isActive() {
        return STATUS_CREATED.equals(status) && !isExpired();
    }

    public boolean canBePaid() {
        return STATUS_CREATED.equals(status) && !isExpired();
    }
}

