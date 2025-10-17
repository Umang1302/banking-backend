package com.nedbank.banking.service;

import com.nedbank.banking.dto.CustomerDetailsRequest;
import com.nedbank.banking.dto.CustomerDetailsResponse;
import com.nedbank.banking.dto.UpdateUserProfileRequest;
import com.nedbank.banking.dto.UserProfileResponse;
import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.Customer;
import com.nedbank.banking.entity.Role;
import com.nedbank.banking.entity.Transaction;
import com.nedbank.banking.entity.User;
import com.nedbank.banking.repository.CustomerRepository;
import com.nedbank.banking.repository.TransactionRepository;
import com.nedbank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get current authenticated user's profile
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        log.debug("Retrieving profile for user: {}", username);
        return mapUserToProfileResponse(user);
    }

    /**
     * Get user profile by username (admin only)
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String username) {
        User currentUser = getCurrentUser();
        
        // Check if current user has permission to view other users
        if (!currentUser.hasPermission("USER_READ") && !currentUser.getUsername().equals(username)) {
            throw new SecurityException("Access denied: You don't have permission to view other users' profiles");
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        log.debug("Admin {} retrieving profile for user: {}", currentUser.getUsername(), username);
        return mapUserToProfileResponse(user);
    }

    /**
     * Update current user's profile
     */
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        User user = getCurrentUser();
        
        log.info("Updating profile for user: {}", user.getUsername());
        
        // Update allowed fields
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email is already taken
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Email is already taken");
            }
            user.setEmail(request.getEmail());
            log.debug("Updated email for user: {}", user.getUsername());
        }
        
        if (request.getMobile() != null && !request.getMobile().equals(user.getMobile())) {
            // Check if mobile is already taken
            Optional<User> existingUser = userRepository.findByMobile(request.getMobile());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Mobile number is already taken");
            }
            user.setMobile(request.getMobile());
            log.debug("Updated mobile for user: {}", user.getUsername());
        }
        
        User updatedUser = userRepository.save(user);
        return mapUserToProfileResponse(updatedUser);
    }

    /**
     * Get all users (admin only)
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("USER_READ")) {
            throw new SecurityException("Access denied: You don't have permission to view all users");
        }
        
        log.debug("Admin {} retrieving all users", currentUser.getUsername());
        return userRepository.findAll().stream()
                .map(this::mapUserToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get users by role (admin only)
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUsersByRole(String roleName) {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("USER_READ")) {
            throw new SecurityException("Access denied: You don't have permission to view users by role");
        }
        
        log.debug("Admin {} retrieving users with role: {}", currentUser.getUsername(), roleName);
        return userRepository.findAll().stream()
                .filter(user -> user.hasRole(roleName))
                .map(this::mapUserToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Submit customer details to complete user profile
     * This moves user from PENDING_DETAILS to PENDING_REVIEW status
     */
    @Transactional
    public CustomerDetailsResponse submitCustomerDetails(CustomerDetailsRequest request) {
        User user = getCurrentUser();
        
        log.info("User {} attempting to submit customer details", user.getUsername());
        
        // Validate user is in correct status to submit customer details
        if (!user.needsCustomerDetails()) {
            throw new IllegalStateException(
                String.format("User %s is not in PENDING_DETAILS status. Current status: %s", 
                             user.getUsername(), user.getStatus())
            );
        }
        
        // Check if user already has customer details linked
        Customer existingCustomer = user.getCustomer();
        boolean isResubmission = false;
        
        if (existingCustomer != null) {
            // Allow resubmission only if the previous submission was rejected
            if (existingCustomer.isRejected()) {
                log.info("User {} is resubmitting customer details after rejection", user.getUsername());
                isResubmission = true;
            } else {
                throw new IllegalStateException(
                    String.format("User %s already has customer details. Customer ID: %d, Status: %s", 
                                 user.getUsername(), existingCustomer.getId(), existingCustomer.getStatus())
                );
            }
        }
        
        // Validate nationalId uniqueness (exclude current customer if resubmitting)
        if (isResubmission) {
            // Check if nationalId belongs to a different customer
            customerRepository.findByNationalId(request.getNationalId()).ifPresent(customer -> {
                if (!customer.getId().equals(existingCustomer.getId())) {
                    throw new IllegalArgumentException("National ID is already registered with another customer");
                }
            });
        } else {
            if (customerRepository.existsByNationalId(request.getNationalId())) {
                throw new IllegalArgumentException("National ID is already registered with another customer");
            }
        }
        
        // Validate email uniqueness (if provided and different from user email)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (isResubmission) {
                customerRepository.findByEmail(request.getEmail()).ifPresent(customer -> {
                    if (!customer.getId().equals(existingCustomer.getId())) {
                        throw new IllegalArgumentException("Email is already registered with another customer");
                    }
                });
            } else {
                if (customerRepository.existsByEmail(request.getEmail())) {
                    throw new IllegalArgumentException("Email is already registered with another customer");
                }
            }
        }
        
        // Validate mobile uniqueness (if provided and different from user mobile)
        if (request.getMobile() != null && !request.getMobile().equals(user.getMobile())) {
            if (isResubmission) {
                customerRepository.findByMobile(request.getMobile()).ifPresent(customer -> {
                    if (!customer.getId().equals(existingCustomer.getId())) {
                        throw new IllegalArgumentException("Mobile number is already registered with another customer");
                    }
                });
            } else {
                if (customerRepository.existsByMobile(request.getMobile())) {
                    throw new IllegalArgumentException("Mobile number is already registered with another customer");
                }
            }
        }
        
        // Convert otherInfo Map to JSON string
        String otherInfoJson = null;
        if (request.getOtherInfo() != null && !request.getOtherInfo().isEmpty()) {
            try {
                otherInfoJson = objectMapper.writeValueAsString(request.getOtherInfo());
            } catch (Exception e) {
                log.warn("Failed to serialize otherInfo to JSON for user: {}", user.getUsername());
                throw new IllegalArgumentException("Invalid additional information format");
            }
        }
        
        // Build full address from request.address or from otherInfo components
        String fullAddress = request.getAddress();
        if (fullAddress == null || fullAddress.trim().isEmpty()) {
            // Try to build address from otherInfo components
            if (request.getOtherInfo() != null) {
                StringBuilder addressBuilder = new StringBuilder();
                
                String streetAddress = (String) request.getOtherInfo().get("streetAddress");
                String city = (String) request.getOtherInfo().get("city");
                String state = (String) request.getOtherInfo().get("state");
                String zipCode = (String) request.getOtherInfo().get("zipCode");
                
                if (streetAddress != null) addressBuilder.append(streetAddress);
                if (city != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    addressBuilder.append(city);
                }
                if (state != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(", ");
                    addressBuilder.append(state);
                }
                if (zipCode != null) {
                    if (addressBuilder.length() > 0) addressBuilder.append(" ");
                    addressBuilder.append(zipCode);
                }
                
                fullAddress = addressBuilder.toString();
            }
        }
        
        // Create or update Customer entity from request
        Customer customer;
        if (isResubmission) {
            // Update existing customer with new details
            customer = existingCustomer;
            customer.setFirstName(request.getFirstName());
            customer.setLastName(request.getLastName());
            customer.setDateOfBirth(request.getDateOfBirth());
            customer.setAddress(fullAddress);
            customer.setNationalId(request.getNationalId());
            customer.setEmail(request.getEmail() != null ? request.getEmail() : user.getEmail());
            customer.setMobile(request.getMobile() != null ? request.getMobile() : user.getMobile());
            customer.setOtherInfo(otherInfoJson); // This replaces the entire otherInfo, removing any rejectionReason
            customer.setStatus(Customer.STATUS_PENDING_REVIEW); // Reset status to pending review
            log.info("Updating existing customer {} for resubmission", customer.getId());
        } else {
            // Create new customer
            customer = Customer.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .dateOfBirth(request.getDateOfBirth())
                    .address(fullAddress)
                    .nationalId(request.getNationalId())
                    .email(request.getEmail() != null ? request.getEmail() : user.getEmail())
                    .mobile(request.getMobile() != null ? request.getMobile() : user.getMobile())
                    .otherInfo(otherInfoJson)
                    .status(Customer.STATUS_PENDING_REVIEW)
                    .build();
        }
        
        // Save customer
        Customer savedCustomer = customerRepository.save(customer);
        
        // Link customer to user and update user status
        user.setCustomer(savedCustomer);
        user.setStatus(User.STATUS_PENDING_REVIEW);
        
        // Update user email/mobile if they provided new ones
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (request.getMobile() != null && !request.getMobile().equals(user.getMobile())) {
            user.setMobile(request.getMobile());
        }
        
        // Save updated user
        User savedUser = userRepository.save(user);
        
        if (isResubmission) {
            log.info("Customer details resubmitted successfully for user: {}. Customer ID: {}, Status: {}", 
                    user.getUsername(), savedCustomer.getId(), savedUser.getStatus());
        } else {
            log.info("Customer details submitted successfully for user: {}. Customer ID: {}, Status: {}", 
                    user.getUsername(), savedCustomer.getId(), savedUser.getStatus());
        }
        
        // Return success response
        return CustomerDetailsResponse.success(
            savedUser.getId(),
            savedCustomer.getId(),
            savedUser.getUsername(),
            savedCustomer.getCustomerNumber(),
            savedCustomer.getFirstName(),
            savedCustomer.getLastName(),
            savedCustomer.getEmail(),
            savedCustomer.getMobile(),
            savedCustomer.getDateOfBirth(),
            savedCustomer.getAddress(),
            savedCustomer.getNationalId()
        );
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
            
            // Add recent transactions (last 10)
            builder.recentTransactions(mapRecentTransactions(user.getCustomer().getId()));
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
                .dateOfBirth(customer.getDateOfBirth())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .mobile(customer.getMobile())
                .address(customer.getAddress())
                .nationalId(customer.getNationalId())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .otherInfo(customer.getOtherInfo()) // Include additional customer details JSON (including rejectionReason if rejected)
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
    
    private List<UserProfileResponse.TransactionInfo> mapRecentTransactions(Long customerId) {
        try {
            List<Transaction> transactions = transactionRepository
                    .findTop10ByCustomerIdOrderByTransactionDateDesc(customerId);
            
            // Limit to 10 transactions
            return transactions.stream()
                    .limit(10)
                    .map(transaction -> UserProfileResponse.TransactionInfo.builder()
                            .transactionId(transaction.getId())
                            .transactionReference(transaction.getTransactionReference())
                            .transactionType(transaction.getTransactionType())
                            .amount(formatCurrency(transaction.getAmount()))
                            .currency(transaction.getCurrency())
                            .accountNumber(transaction.getAccount().getAccountNumber())
                            .description(transaction.getDescription())
                            .category(transaction.getCategory())
                            .status(transaction.getStatus())
                            .transactionDate(transaction.getTransactionDate())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch recent transactions for customer {}: {}", customerId, e.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<Account> getCustomerAccounts() {
        User user = getCurrentUser();
        log.debug("Getting accounts for user: {}", user.getUsername());
        
        // Check if user has a customer linked
        if (user.getCustomer() == null) {
            log.warn("User {} does not have a customer account linked", user.getUsername());
            throw new IllegalStateException("No customer account found for this user. Please complete customer registration first.");
        }
        
        Customer customer = user.getCustomer();
        log.debug("Found customer {} with ID: {}", customer.getCustomerNumber(), customer.getId());
        
        // Get accounts
        List<Account> accounts = customer.getAccounts().stream().collect(Collectors.toList());
        log.debug("Retrieved {} accounts for customer {}", accounts.size(), customer.getCustomerNumber());
        
        return accounts;
    }
}
