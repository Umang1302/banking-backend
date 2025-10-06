package com.nedbank.banking.service;

import com.nedbank.banking.dto.UserProfileResponse;
import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.Customer;
import com.nedbank.banking.entity.Role;
import com.nedbank.banking.entity.User;
import com.nedbank.banking.repository.AccountRepository;
import com.nedbank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * Get users by status (admin only)
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUsersByStatus(String status) {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("USER_READ")) {
            throw new SecurityException("Access denied: You don't have permission to view users by status");
        }
        
        log.debug("Admin {} retrieving users with status: {}", currentUser.getUsername(), status);
        return userRepository.findByStatus(status).stream()
                .map(this::mapUserToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get user by ID (admin only)
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long userId) {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("USER_READ")) {
            throw new SecurityException("Access denied: You don't have permission to view user details");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        log.debug("Admin {} retrieving user details for ID: {}", currentUser.getUsername(), userId);
        return mapUserToProfileResponse(user);
    }

    /**
     * Approve a user (move from PENDING_REVIEW to ACTIVE)
     */
    @Transactional
    public UserProfileResponse approveUser(Long userId) {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("USER_WRITE")) {
            throw new SecurityException("Access denied: You don't have permission to approve users");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Validate user is in correct status to be approved
        if (!user.isPendingReview()) {
            throw new IllegalStateException(
                String.format("User %s is not in PENDING_REVIEW status. Current status: %s", 
                             user.getUsername(), user.getStatus())
            );
        }
        
        // Update user status to ACTIVE
        user.setStatus(User.STATUS_ACTIVE);
        
        // If user has customer details, also approve customer
        if (user.getCustomer() != null) {
            Customer customer = user.getCustomer();
            customer.setStatus(Customer.STATUS_ACTIVE);
            
            // Create default savings account for newly approved customer
            createDefaultAccount(customer);
        }
        
        User approvedUser = userRepository.save(user);
        
        log.info("User {} approved by admin {}. Status changed to: {}", 
                user.getUsername(), currentUser.getUsername(), approvedUser.getStatus());
        
        return mapUserToProfileResponse(approvedUser);
    }

    /**
     * Reject a user (move from PENDING_REVIEW to REJECTED)
     */
    @Transactional
    public UserProfileResponse rejectUser(Long userId, String reason) {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("USER_WRITE")) {
            throw new SecurityException("Access denied: You don't have permission to reject users");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Validate user is in correct status to be rejected
        if (!user.isPendingReview()) {
            throw new IllegalStateException(
                String.format("User %s is not in PENDING_REVIEW status. Current status: %s", 
                             user.getUsername(), user.getStatus())
            );
        }
        
        // Update user status to REJECTED
        user.setStatus(User.STATUS_REJECTED);
        
        // If user has customer details, also reject customer
        if (user.getCustomer() != null) {
            Customer customer = user.getCustomer();
            customer.setStatus(Customer.STATUS_REJECTED);
        }
        
        User rejectedUser = userRepository.save(user);
        
        log.info("User {} rejected by admin {}. Status changed to: {}. Reason: {}", 
                user.getUsername(), currentUser.getUsername(), rejectedUser.getStatus(), reason);
        
        return mapUserToProfileResponse(rejectedUser);
    }

    /**
     * Get admin dashboard data with user statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminDashboardData() {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("USER_READ")) {
            throw new SecurityException("Access denied: You don't have permission to view admin dashboard");
        }
        
        Map<String, Object> dashboardData = new HashMap<>();
        
        // User statistics by status
        Map<String, Long> userStatusCounts = new HashMap<>();
        userStatusCounts.put("PENDING_DETAILS", userRepository.countByStatus(User.STATUS_PENDING_DETAILS));
        userStatusCounts.put("PENDING_REVIEW", userRepository.countByStatus(User.STATUS_PENDING_REVIEW));
        userStatusCounts.put("UNDER_REVIEW", userRepository.countByStatus(User.STATUS_UNDER_REVIEW));
        userStatusCounts.put("ACTIVE", userRepository.countByStatus(User.STATUS_ACTIVE));
        userStatusCounts.put("REJECTED", userRepository.countByStatus(User.STATUS_REJECTED));
        userStatusCounts.put("SUSPENDED", userRepository.countByStatus(User.STATUS_SUSPENDED));
        userStatusCounts.put("LOCKED", userRepository.countByStatus(User.STATUS_LOCKED));
        userStatusCounts.put("EXPIRED", userRepository.countByStatus(User.STATUS_EXPIRED));
        
        dashboardData.put("userStatusCounts", userStatusCounts);
        dashboardData.put("totalUsers", userRepository.count());
        
        // Customer statistics
        long totalCustomers = userRepository.countUsersWithCustomers();
        dashboardData.put("totalCustomers", totalCustomers);
        
        // Account statistics
        long totalAccounts = accountRepository.count();
        dashboardData.put("totalAccounts", totalAccounts);
        
        // Pending actions (items requiring admin attention)
        Map<String, Object> pendingActions = new HashMap<>();
        pendingActions.put("pendingDetailsCount", userStatusCounts.get("PENDING_DETAILS"));
        pendingActions.put("pendingReviewCount", userStatusCounts.get("PENDING_REVIEW"));
        pendingActions.put("underReviewCount", userStatusCounts.get("UNDER_REVIEW"));
        dashboardData.put("pendingActions", pendingActions);
        
        // Recent activity (last 10 users)
        List<UserProfileResponse> recentUsers = userRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::mapUserToProfileResponse)
                .collect(Collectors.toList());
        dashboardData.put("recentUsers", recentUsers);
        
        log.debug("Admin {} retrieved dashboard data", currentUser.getUsername());
        return dashboardData;
    }

    /**
     * Create default savings account for newly approved customer
     */
    private void createDefaultAccount(Customer customer) {
        try {
            Account defaultAccount = Account.builder()
                    .customer(customer)
                    .accountType("SAVINGS")
                    .currency("USD")
                    .balance(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .minimumBalance(BigDecimal.valueOf(10.00))
                    .interestRate(BigDecimal.valueOf(0.5))
                    .status("ACTIVE")
                    .build();
            
            accountRepository.save(defaultAccount);
            log.info("Created default savings account for customer: {}", customer.getCustomerNumber());
        } catch (Exception e) {
            log.warn("Failed to create default account for customer {}: {}", 
                    customer.getCustomerNumber(), e.getMessage());
            // Don't throw exception, just log warning
        }
    }

    private User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found: " + username));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private UserProfileResponse mapUserToProfileResponse(User user) {
        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(mapRoles(user.getRoles()))
                .permissions(mapPermissions(user.getRoles()));

        // Add customer information if user is linked to a customer
        if (user.getCustomer() != null) {
            builder.customer(mapCustomerInfo(user.getCustomer()));
            builder.accounts(mapAccountsInfo(user.getCustomer().getAccounts()));
        }

        return builder.build();
    }

    private Set<UserProfileResponse.RoleInfo> mapRoles(Set<Role> roles) {
        return roles.stream()
                .map(role -> UserProfileResponse.RoleInfo.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .description(role.getDescription())
                        .build())
                .collect(Collectors.toSet());
    }

    private Set<String> mapPermissions(Set<Role> roles) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());
    }

    private UserProfileResponse.CustomerInfo mapCustomerInfo(Customer customer) {
        return UserProfileResponse.CustomerInfo.builder()
                .customerId(customer.getId())
                .customerNumber(customer.getCustomerNumber())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .mobile(customer.getMobile())
                .address(customer.getAddress())
                .nationalId(customer.getNationalId())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .otherInfo(customer.getOtherInfo())
                .build();
    }

    private List<UserProfileResponse.AccountInfo> mapAccountsInfo(Set<Account> accounts) {
        return accounts.stream()
                .map(account -> UserProfileResponse.AccountInfo.builder()
                        .accountId(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .accountType(account.getAccountType())
                        .balance(formatCurrency(account.getBalance()))
                        .availableBalance(formatCurrency(account.getAvailableBalance()))
                        .currency(account.getCurrency())
                        .status(account.getStatus())
                        .interestRate(account.getInterestRate() != null ? account.getInterestRate().toString() + "%" : null)
                        .minimumBalance(formatCurrency(account.getMinimumBalance()))
                        .createdAt(account.getCreatedAt())
                        .lastTransactionDate(account.getLastTransactionDate())
                        .build())
                .collect(Collectors.toList());
    }

    private String formatCurrency(BigDecimal amount) {
        return amount != null ? String.format("%.2f", amount) : "0.00";
    }
}
