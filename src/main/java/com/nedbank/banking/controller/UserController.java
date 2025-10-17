package com.nedbank.banking.controller;

import com.nedbank.banking.dto.CustomerDetailsRequest;
import com.nedbank.banking.dto.CustomerDetailsResponse;
import com.nedbank.banking.dto.UpdateUserProfileRequest;
import com.nedbank.banking.dto.UserProfileResponse;
import com.nedbank.banking.entity.Account;
import com.nedbank.banking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        try {
            log.debug("Getting current user profile");
            UserProfileResponse profile = userService.getCurrentUserProfile();
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error retrieving current user profile: {}", e.getMessage());
            throw e;
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateCurrentUserProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        try {
            log.debug("Updating current user profile");
            UserProfileResponse profile = userService.updateCurrentUserProfile(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("profile", profile);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Profile update validation error: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error updating user profile: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update profile");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Submit customer details to complete user profile
     * Available to authenticated users in PENDING_DETAILS status
     */
    @PostMapping("/customer-details")
    public ResponseEntity<?> submitCustomerDetails(@Valid @RequestBody CustomerDetailsRequest request) {
        try {
            log.debug("Submitting customer details for current user");
            CustomerDetailsResponse response = userService.submitCustomerDetails(request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            log.warn("Customer details submission failed - invalid user state: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            error.put("code", "INVALID_USER_STATE");
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (IllegalArgumentException e) {
            log.warn("Customer details submission failed - validation error: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            error.put("code", "VALIDATION_ERROR");
            
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error submitting customer details: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to submit customer details. Please try again later.");
            error.put("status", "error");
            error.put("code", "INTERNAL_ERROR");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get user profile by username
     * Requires USER_READ permission (Admin/Superadmin) or user accessing own profile
     */
    @GetMapping("/profile/{username}")
    @PreAuthorize("hasAuthority('USER_READ') or #username == authentication.name")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) {
        try {
            log.debug("Getting profile for user: {}", username);
            UserProfileResponse profile = userService.getUserProfile(username);
            return ResponseEntity.ok(profile);
        } catch (SecurityException e) {
            log.warn("Access denied for user profile request: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Access denied");
            error.put("status", "error");
            
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            log.error("Error retrieving user profile for {}: {}", username, e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "User not found");
            error.put("status", "error");
            
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<?> getAllUsers() {
        try {
            log.debug("Getting all users");
            List<UserProfileResponse> users = userService.getAllUsers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("count", users.size());
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("Access denied for all users request: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Access denied");
            error.put("status", "error");
            
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            log.error("Error retrieving all users: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to retrieve users");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<?> getUsersByRole(@PathVariable String roleName) {
        try {
            log.debug("Getting users with role: {}", roleName);
            List<UserProfileResponse> users = userService.getUsersByRole(roleName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("role", roleName);
            response.put("count", users.size());
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("Access denied for users by role request: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Access denied");
            error.put("status", "error");
            
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            log.error("Error retrieving users by role {}: {}", roleName, e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to retrieve users");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get user's dashboard data based on their roles
     * Returns role-specific information for dashboard rendering
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        try {
            log.debug("Getting dashboard data for current user");
            UserProfileResponse profile = userService.getCurrentUserProfile();
            
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("user", profile);
            
            // Add role-specific dashboard data
            Set<String> permissions = profile.getPermissions();
            Map<String, Boolean> dashboardPermissions = new HashMap<>();
            
            // Dashboard access permissions
            dashboardPermissions.put("customerDashboard", permissions.contains("DASHBOARD_CUSTOMER"));
            dashboardPermissions.put("accountantDashboard", permissions.contains("DASHBOARD_ACCOUNTANT"));
            dashboardPermissions.put("adminDashboard", permissions.contains("DASHBOARD_ADMIN"));
            dashboardPermissions.put("superadminDashboard", permissions.contains("DASHBOARD_SUPERADMIN"));
            
            // Feature access permissions
            dashboardPermissions.put("canViewAllUsers", permissions.contains("USER_READ"));
            dashboardPermissions.put("canManageUsers", permissions.contains("USER_WRITE"));
            dashboardPermissions.put("canViewReports", permissions.contains("REPORTS_BASIC") || permissions.contains("REPORTS_ADVANCED"));
            dashboardPermissions.put("canViewAccounts", permissions.contains("ACCOUNT_READ"));
            dashboardPermissions.put("canManageAccounts", permissions.contains("ACCOUNT_WRITE"));
            dashboardPermissions.put("canViewTransactions", permissions.contains("TRANSACTION_READ"));
            dashboardPermissions.put("canCreateTransactions", permissions.contains("TRANSACTION_WRITE"));
            
            dashboardData.put("permissions", dashboardPermissions);
            dashboardData.put("roles", profile.getRoles());
            
            // Add account summary for customers
            if (profile.getCustomer() != null && profile.getAccounts() != null) {
                Map<String, Object> accountSummary = new HashMap<>();
                accountSummary.put("totalAccounts", profile.getAccounts().size());
                accountSummary.put("accounts", profile.getAccounts());
                dashboardData.put("accountSummary", accountSummary);
            }
            
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            log.error("Error retrieving dashboard data: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to retrieve dashboard data");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    //create an API to get Customer Accounts accounts/customer
    @GetMapping("/accounts/customer")
    public ResponseEntity<?> getCustomerAccounts() {
        try {
            log.debug("Getting customer accounts");
            List<Account> accounts = userService.getCustomerAccounts();
            return ResponseEntity.ok(accounts);
        }
        catch (Exception e) {
            log.error("Error getting customer accounts: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
