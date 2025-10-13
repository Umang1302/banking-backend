package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    
    private Long userId;
    private String username;
    private String email;
    private String mobile;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Role and Permission information
    private Set<RoleInfo> roles;
    private Set<String> permissions;
    
    // Customer information (if linked to a customer)
    private CustomerInfo customer;
    
    // Account information (if customer)
    private List<AccountInfo> accounts;
    
    // Recent transactions (last 10)
    private List<TransactionInfo> recentTransactions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        private Long id;
        private String name;
        private String description;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private Long customerId;
        private LocalDateTime dateOfBirth;
        private String customerNumber;
        private String firstName;
        private String lastName;
        private String email;
        private String mobile;
        private String address;
        private String nationalId;
        private String status;
        private LocalDateTime createdAt;
        private String otherInfo; // JSON string containing additional customer details (including rejectionReason if rejected)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountInfo {
        private Long accountId;
        private String accountNumber;
        private String accountType;
        private String balance;
        private String availableBalance;
        private String currency;
        private String status;
        private String interestRate;
        private String minimumBalance;
        private LocalDateTime createdAt;
        private LocalDateTime lastTransactionDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionInfo {
        private Long transactionId;
        private String transactionReference;
        private String transactionType;
        private String amount;
        private String currency;
        private String accountNumber;
        private String description;
        private String category;
        private String status;
        private LocalDateTime transactionDate;
    }
}
