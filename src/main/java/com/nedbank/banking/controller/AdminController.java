package com.nedbank.banking.controller;

import com.nedbank.banking.dto.UserProfileResponse;
import com.nedbank.banking.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('USER_READ')")
public class AdminController {

    private final AdminService adminService;

    /**
     * Get all users with PENDING_DETAILS status
     * Requires USER_READ permission (Admin/Superadmin)
     */
    @GetMapping("/pending-details")
    public ResponseEntity<?> getPendingDetailsUsers() {
        try {
            log.debug("Admin retrieving all users with PENDING_DETAILS status");
            List<UserProfileResponse> pendingUsers = adminService.getUsersByStatus("PENDING_DETAILS");
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", pendingUsers);
            response.put("count", pendingUsers.size());
            response.put("status", "PENDING_DETAILS");
            response.put("message", "Users who need to complete their customer details");
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("Access denied for pending details request: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Access denied");
            error.put("status", "error");
            
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            log.error("Error retrieving pending details users: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to retrieve pending details users");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get all users with PENDING_REVIEW status (submitted customer details, awaiting approval)
     * Requires USER_READ permission (Admin/Superadmin)
     */
    @GetMapping("/pending-review")
    public ResponseEntity<?> getPendingReviewUsers() {
        try {
            log.debug("Admin retrieving all users with PENDING_REVIEW status");
            List<UserProfileResponse> pendingUsers = adminService.getUsersByStatus("PENDING_REVIEW");
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", pendingUsers);
            response.put("count", pendingUsers.size());
            response.put("status", "PENDING_REVIEW");
            response.put("message", "Users waiting for admin approval");
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("Access denied for pending review request: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Access denied");
            error.put("status", "error");
            
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            log.error("Error retrieving pending review users: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to retrieve pending review users");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Approve a user's customer details (move from PENDING_REVIEW to ACTIVE)
     * Requires USER_WRITE permission (Admin/Superadmin)
     */
    @PostMapping("/approve-user/{userId}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        try {
            log.info("Admin approving user with ID: {}", userId);
            UserProfileResponse approvedUser = adminService.approveUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User approved successfully");
            response.put("user", approvedUser);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("User approval failed - invalid state: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            error.put("code", "INVALID_USER_STATE");
            
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalArgumentException e) {
            log.warn("User approval failed - user not found: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            error.put("code", "USER_NOT_FOUND");
            
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            log.warn("Access denied for user approval: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Access denied");
            error.put("status", "error");
            
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            log.error("Error approving user {}: {}", userId, e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to approve user");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Reject a user's customer details (move from PENDING_REVIEW to REJECTED)
     * Requires USER_WRITE permission (Admin/Superadmin)
     */
    @PostMapping("/reject-user/{userId}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId, @RequestBody Map<String, String> requestBody) {
        try {
            String reason = requestBody.get("reason");
            log.info("Admin rejecting user with ID: {} with reason: {}", userId, reason);
            
            UserProfileResponse rejectedUser = adminService.rejectUser(userId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User rejected successfully");
            response.put("user", rejectedUser);
            response.put("status", "success");
            response.put("reason", reason);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("User rejection failed - invalid state: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            error.put("code", "INVALID_USER_STATE");
            
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalArgumentException e) {
            log.warn("User rejection failed - user not found: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            error.put("code", "USER_NOT_FOUND");
            
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            log.warn("Access denied for user rejection: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Access denied");
            error.put("status", "error");
            
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            log.error("Error rejecting user {}: {}", userId, e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to reject user");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get admin dashboard data with user statistics
     * Requires USER_READ permission (Admin/Superadmin)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getAdminDashboard() {
        try {
            log.debug("Admin retrieving dashboard data");
            Map<String, Object> dashboardData = adminService.getAdminDashboardData();
            
            return ResponseEntity.ok(dashboardData);
        } catch (SecurityException e) {
            log.warn("Access denied for admin dashboard: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Access denied");
            error.put("status", "error");
            
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            log.error("Error retrieving admin dashboard: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to retrieve dashboard data");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get user details by ID (for admin review)
     * Requires USER_READ permission (Admin/Superadmin)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserDetails(@PathVariable Long userId) {
        try {
            log.debug("Admin retrieving user details for ID: {}", userId);
            UserProfileResponse user = adminService.getUserById(userId);
            
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.warn("User not found: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            log.warn("Access denied for user details: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Access denied");
            error.put("status", "error");
            
            return ResponseEntity.status(403).body(error);
        } catch (Exception e) {
            log.error("Error retrieving user details for {}: {}", userId, e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to retrieve user details");
            error.put("status", "error");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
