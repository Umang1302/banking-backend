package com.nedbank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_transaction_type", columnList = "transaction_type"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    // Transaction Type Constants
    public static final String TYPE_DEBIT = "DEBIT";
    public static final String TYPE_CREDIT = "CREDIT";
    public static final String TYPE_TRANSFER = "TRANSFER";
    public static final String TYPE_WITHDRAWAL = "WITHDRAWAL";
    public static final String TYPE_DEPOSIT = "DEPOSIT";
    public static final String TYPE_FEE = "FEE";
    public static final String TYPE_INTEREST = "INTEREST";

    // Transaction Status Constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_REVERSED = "REVERSED";

    // Transaction Category Constants
    public static final String CATEGORY_PAYMENT = "PAYMENT";
    public static final String CATEGORY_TRANSFER = "TRANSFER";
    public static final String CATEGORY_WITHDRAWAL = "WITHDRAWAL";
    public static final String CATEGORY_DEPOSIT = "DEPOSIT";
    public static final String CATEGORY_BILL_PAYMENT = "BILL_PAYMENT";
    public static final String CATEGORY_SALARY = "SALARY";
    public static final String CATEGORY_REFUND = "REFUND";
    public static final String CATEGORY_OTHER = "OTHER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", unique = true, nullable = false, length = 50)
    private String transactionReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "balance_before", precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "value_date")
    private LocalDateTime valueDate;

    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "is_bulk_upload")
    @Builder.Default
    private Boolean isBulkUpload = false;

    @Column(name = "bulk_upload_batch_id", length = 50)
    private String bulkUploadBatchId;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

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
        // Only set transactionDate if not already set (to preserve historical dates from DataSeeder)
        if (transactionDate == null) {
            transactionDate = now;
        }
        if (transactionReference == null) {
            transactionReference = generateTransactionReference();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateTransactionReference() {
        // Generate unique transaction reference (max 50 chars)
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 10000);
        return "TXN" + timestamp + String.format("%04d", random);
    }

    // Helper methods for transaction type checks
    public boolean isDebit() {
        return TYPE_DEBIT.equals(transactionType) || TYPE_WITHDRAWAL.equals(transactionType);
    }

    public boolean isCredit() {
        return TYPE_CREDIT.equals(transactionType) || TYPE_DEPOSIT.equals(transactionType);
    }

    public boolean isTransfer() {
        return TYPE_TRANSFER.equals(transactionType);
    }

    // Helper methods for status checks
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
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

    public boolean canBeProcessed() {
        return STATUS_PENDING.equals(status);
    }

    public boolean canBeCancelled() {
        return STATUS_PENDING.equals(status) || STATUS_PROCESSING.equals(status);
    }
}

