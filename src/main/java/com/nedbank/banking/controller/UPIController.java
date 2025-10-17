package com.nedbank.banking.controller;

import com.nedbank.banking.dto.*;
import com.nedbank.banking.entity.UPIAccount;
import com.nedbank.banking.service.UPIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for UPI operations
 * Handles UPI ID registration, payment initiation, and management
 */
@RestController
@RequestMapping("/api/upi")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UPIController {

    private final UPIService upiService;

    /**
     * Register a new UPI ID
     * POST /api/upi/register
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> registerUPI(@Valid @RequestBody UPIRegisterRequest request) {
        try {
            log.info("Received UPI registration request: {}", request.getUpiId());
            
            UPIRegisterResponse response = upiService.registerUPI(request);
            
            if (!response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityException e) {
            log.error("Security error registering UPI: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(UPIRegisterResponse.failure("Security Error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Validation error registering UPI: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(UPIRegisterResponse.failure("Validation Error: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error registering UPI: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UPIRegisterResponse.failure("Failed to register UPI ID"));
        }
    }

    /**
     * Get all UPI accounts for current user
     * GET /api/upi/accounts
     */
    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> getMyUPIAccounts() {
        try {
            log.info("Fetching UPI accounts for current user");
            
            List<UPIAccountResponse> responses = upiService.getMyUPIAccounts();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error fetching UPI accounts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to fetch UPI accounts"));
        }
    }

    /**
     * Get UPI account details by UPI ID
     * GET /api/upi/accounts/{upiId}
     */
    @GetMapping("/accounts/{upiId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> getUPIAccountByUpiId(@PathVariable String upiId) {
        try {
            log.info("Fetching UPI account: {}", upiId);
            
            UPIAccountResponse response = upiService.getUPIAccountByUpiId(upiId);
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.error("Security error fetching UPI account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Security Error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("UPI account not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching UPI account: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to fetch UPI account"));
        }
    }

    /**
     * Process UPI payment (direct internal transfer)
     * POST /api/upi/pay
     */
    @PostMapping("/pay")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> processUPIPayment(@Valid @RequestBody UPIPaymentRequest request) {
        try {
            log.info("Processing UPI payment to: {}, amount: {}", 
                    request.getReceiverUpiId(), request.getAmount());
            
            UPIPaymentResponse response = upiService.initiateUPIPayment(request);
            
            if (!response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.error("Security error processing UPI payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(UPIPaymentResponse.failure("Security Error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Validation error processing UPI payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(UPIPaymentResponse.failure("Validation Error: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing UPI payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UPIPaymentResponse.failure("Failed to process UPI payment"));
        }
    }

    /**
     * Deactivate UPI ID
     * DELETE /api/upi/accounts/{upiId}
     */
    @DeleteMapping("/accounts/{upiId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> deactivateUPI(@PathVariable String upiId) {
        try {
            log.info("Deactivating UPI ID: {}", upiId);
            
            upiService.deactivateUPI(upiId);
            
            return ResponseEntity.ok(new SuccessResponse("UPI ID deactivated successfully"));
        } catch (SecurityException e) {
            log.error("Security error deactivating UPI: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Security Error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("UPI ID not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deactivating UPI: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to deactivate UPI ID"));
        }
    }

    /**
     * Set UPI as primary
     * PUT /api/upi/accounts/{upiId}/set-primary
     */
    @PutMapping("/accounts/{upiId}/set-primary")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> setPrimaryUPI(@PathVariable String upiId) {
        try {
            log.info("Setting UPI ID as primary: {}", upiId);
            
            upiService.setPrimaryUPI(upiId);
            
            return ResponseEntity.ok(new SuccessResponse("UPI ID set as primary successfully"));
        } catch (SecurityException e) {
            log.error("Security error setting primary UPI: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Security Error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("UPI ID not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error setting primary UPI: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to set primary UPI"));
        }
    }

    /**
     * Get UPI transaction history for an account
     * GET /api/upi/transactions/{accountNumber}
     */
    @GetMapping("/transactions/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> getUPITransactionHistory(@PathVariable String accountNumber) {
        try {
            log.info("Fetching UPI transaction history for account: {}", accountNumber);
            
            List<QRTransactionResponse> responses = upiService.getUPITransactionHistory(accountNumber);
            
            return ResponseEntity.ok(responses);
        } catch (SecurityException e) {
            log.error("Security error fetching UPI transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Security Error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Account not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching UPI transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to fetch UPI transactions"));
        }
    }

    /**
     * Validate UPI ID format
     * GET /api/upi/validate/{upiId}
     */
    @GetMapping("/validate/{upiId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> validateUPIId(@PathVariable String upiId) {
        try {
            log.info("Validating UPI ID format: {}", upiId);
            
            // Basic UPI ID format validation
            boolean isValid = upiId.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$");
            
            if (isValid) {
                String provider = UPIAccount.extractProvider(upiId);
                return ResponseEntity.ok(new UPIValidationResponse(true, "Valid UPI ID format", provider));
            } else {
                return ResponseEntity.ok(new UPIValidationResponse(false, "Invalid UPI ID format", null));
            }
        } catch (Exception e) {
            log.error("Error validating UPI ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to validate UPI ID"));
        }
    }

    /**
     * Response DTOs
     */
    private record ErrorResponse(String error, String message) {}
    private record SuccessResponse(String message) {}
    private record UPIValidationResponse(boolean valid, String message, String provider) {}
}

