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
import java.time.LocalTime;

/**
 * Entity to track EFT (Electronic Funds Transfer) transactions
 * Supports NEFT, RTGS, and IMPS
 */
@Entity
@Table(name = "eft_transactions", indexes = {
    @Index(name = "idx_eft_reference", columnList = "eft_reference"),
    @Index(name = "idx_eft_type", columnList = "eft_type"),
    @Index(name = "idx_eft_status", columnList = "status"),
    @Index(name = "idx_batch_id", columnList = "batch_id"),
    @Index(name = "idx_source_account", columnList = "source_account_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EFTTransaction {

    // EFT Type Constants
    public static final String TYPE_NEFT = "NEFT";
    public static final String TYPE_RTGS = "RTGS";
    public static final String TYPE_IMPS = "IMPS";

    // Status Constants
    public static final String STATUS_PENDING = "PENDING";           // Waiting for batch
    public static final String STATUS_QUEUED = "QUEUED";            // In batch queue
    public static final String STATUS_PROCESSING = "PROCESSING";     // Being processed
    public static final String STATUS_COMPLETED = "COMPLETED";       // Successfully completed
    public static final String STATUS_FAILED = "FAILED";            // Failed
    public static final String STATUS_CANCELLED = "CANCELLED";       // Cancelled by user

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eft_reference", unique = true, nullable = false, length = 50)
    private String eftReference;

    @Column(name = "eft_type", nullable = false, length = 10)
    private String eftType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = false)
    @JsonIgnore
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    @JsonIgnore
    private Beneficiary beneficiary;

    @Column(name = "beneficiary_account_number", nullable = false, length = 20)
    private String beneficiaryAccountNumber;

    @Column(name = "beneficiary_name", nullable = false, length = 100)
    private String beneficiaryName;

    @Column(name = "beneficiary_ifsc", nullable = false, length = 11)
    private String beneficiaryIfsc;

    @Column(name = "beneficiary_bank_name", length = 100)
    private String beneficiaryBankName;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "charges", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal charges = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "batch_time")
    private LocalTime batchTime;

    @Column(name = "estimated_completion")
    private LocalDateTime estimatedCompletion;

    @Column(name = "actual_completion")
    private LocalDateTime actualCompletion;

    @Column(name = "initiated_by", nullable = false, length = 100)
    private String initiatedBy;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // Linked internal transaction (for debit from source account)
    @OneToOne
    @JoinColumn(name = "transaction_id")
    @JsonIgnore
    private Transaction transaction;

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
        if (eftReference == null) {
            eftReference = generateEFTReference();
        }
        // Calculate total amount
        if (totalAmount == null && amount != null && charges != null) {
            totalAmount = amount.add(charges);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateEFTReference() {
        // Generate unique EFT reference
        String prefix = switch (eftType) {
            case TYPE_NEFT -> "NEFT";
            case TYPE_RTGS -> "RTGS";
            case TYPE_IMPS -> "IMPS";
            default -> "EFT";
        };
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 10000);
        return prefix + timestamp + String.format("%04d", random);
    }

    // Helper methods
    public boolean isNEFT() {
        return TYPE_NEFT.equals(eftType);
    }

    public boolean isRTGS() {
        return TYPE_RTGS.equals(eftType);
    }

    public boolean isIMPS() {
        return TYPE_IMPS.equals(eftType);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isQueued() {
        return STATUS_QUEUED.equals(status);
    }

    public boolean isProcessing() {
        return STATUS_PROCESSING.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    public boolean canBeCancelled() {
        return STATUS_PENDING.equals(status) || STATUS_QUEUED.equals(status);
    }
}

