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
 * Entity to track QR and UPI payment transactions
 */
@Entity
@Table(name = "qr_transactions", indexes = {
    @Index(name = "idx_qr_txn_reference", columnList = "transaction_reference"),
    @Index(name = "idx_qr_txn_status", columnList = "status"),
    @Index(name = "idx_qr_txn_type", columnList = "payment_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QRTransaction {

    // Payment Type Constants
    public static final String TYPE_QR_CODE = "QR_CODE";
    public static final String TYPE_UPI = "UPI";

    // Status Constants
    public static final String STATUS_INITIATED = "INITIATED";       // Payment initiated
    public static final String STATUS_PENDING = "PENDING";          // Waiting for payment confirmation
    public static final String STATUS_AUTHORIZED = "AUTHORIZED";     // Payment authorized
    public static final String STATUS_CAPTURED = "CAPTURED";        // Payment captured
    public static final String STATUS_SETTLED = "SETTLED";          // Internal transfer completed
    public static final String STATUS_FAILED = "FAILED";            // Payment failed
    public static final String STATUS_REFUNDED = "REFUNDED";        // Payment refunded

    // Payment Method Constants
    public static final String METHOD_UPI = "UPI";
    public static final String METHOD_CARD = "CARD";
    public static final String METHOD_NETBANKING = "NETBANKING";
    public static final String METHOD_WALLET = "WALLET";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", unique = true, nullable = false, length = 50)
    private String transactionReference;

    @Column(name = "payment_type", nullable = false, length = 20)
    private String paymentType;  // QR_CODE or UPI

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_request_id")
    @JsonIgnore
    private QRPaymentRequest qrRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upi_account_id")
    @JsonIgnore
    private UPIAccount upiAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_account_id", nullable = false)
    @JsonIgnore
    private Account payerAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_account_id", nullable = false)
    @JsonIgnore
    private Account receiverAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_INITIATED;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;  // UPI, CARD, NETBANKING, WALLET

    @Column(name = "payment_method_details", columnDefinition = "TEXT")
    private String paymentMethodDetails;  // JSON string with additional details

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "initiated_by", nullable = false, length = 100)
    private String initiatedBy;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // Link to internal transaction (debit from payer)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debit_transaction_id")
    @JsonIgnore
    private Transaction debitTransaction;

    // Link to internal transaction (credit to receiver)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_transaction_id")
    @JsonIgnore
    private Transaction creditTransaction;

    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

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
        if (initiatedAt == null) {
            initiatedAt = now;
        }
        if (transactionReference == null) {
            transactionReference = generateTransactionReference();
        }
        // Set net amount to equal amount (no fees)
        if (netAmount == null && amount != null) {
            netAmount = amount;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateTransactionReference() {
        String prefix = TYPE_QR_CODE.equals(paymentType) ? "QRTXN" : "UPITXN";
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 10000);
        return prefix + timestamp + String.format("%04d", random);
    }

    // Helper methods
    public boolean isQRPayment() {
        return TYPE_QR_CODE.equals(paymentType);
    }

    public boolean isUPIPayment() {
        return TYPE_UPI.equals(paymentType);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isSettled() {
        return STATUS_SETTLED.equals(status);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    public boolean canBeSettled() {
        return STATUS_AUTHORIZED.equals(status) || STATUS_CAPTURED.equals(status);
    }
}

