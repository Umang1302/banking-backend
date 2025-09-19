package com.nedbank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@EqualsAndHashCode(exclude = {"customer"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType; // SAVINGS, CURRENT, FIXED_DEPOSIT, etc.

    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, CLOSED, SUSPENDED, DORMANT

    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "minimum_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minimumBalance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (accountNumber == null) {
            accountNumber = generateAccountNumber();
        }
        availableBalance = balance;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateAccountNumber() {
        // Generate unique account number (max 20 chars)
        long timestamp = System.currentTimeMillis() % 1000000000L; // Last 9 digits
        int random = (int)(Math.random() * 1000); // 3 digits max
        return "ACC" + timestamp + String.format("%03d", random);
    }

    // Helper methods for account operations
    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            balance = balance.add(amount);
            availableBalance = availableBalance.add(amount);
            lastTransactionDate = LocalDateTime.now();
        }
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 && availableBalance.compareTo(amount) >= 0) {
            balance = balance.subtract(amount);
            availableBalance = availableBalance.subtract(amount);
            lastTransactionDate = LocalDateTime.now();
        } else {
            throw new RuntimeException("Insufficient balance");
        }
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
