package com.nedbank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customers")
@Data
@EqualsAndHashCode(exclude = {"accounts", "users"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    // Customer Status Constants
    public static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_SUSPENDED = "SUSPENDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_number", unique = true, nullable = false, length = 20)
    private String customerNumber;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;

    @Column(length = 20, unique = true)
    private String mobile;

    @Column(length = 100, unique = true)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "national_id", length = 50, unique = true)
    private String nationalId;

    @Column(name = "other_info", columnDefinition = "TEXT")
    private String otherInfo;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = STATUS_PENDING_REVIEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Account> accounts = new HashSet<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (customerNumber == null) {
            customerNumber = generateCustomerNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateCustomerNumber() {
        // Generate unique 6-digit customer number (100000 to 999999)
        int randomNumber = 100000 + (int)(Math.random() * 900000);
        return String.valueOf(randomNumber);
    }

    // Helper methods
    public void addAccount(Account account) {
        accounts.add(account);
        account.setCustomer(this);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setCustomer(null);
    }

    public void addUser(User user) {
        users.add(user);
        user.setCustomer(this);
    }

    public void removeUser(User user) {
        users.remove(user);
        user.setCustomer(null);
    }

    // Helper methods for status management
    public boolean isPendingReview() {
        return STATUS_PENDING_REVIEW.equals(status);
    }

    public boolean isUnderReview() {
        return STATUS_UNDER_REVIEW.equals(status);
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }

    public boolean isFullyActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }

    public boolean isSuspended() {
        return STATUS_SUSPENDED.equals(status);
    }

    public boolean canHaveBankAccounts() {
        return STATUS_ACTIVE.equals(status);
    }
}
