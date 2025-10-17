package com.nedbank.banking.controller;

import com.nedbank.banking.dto.MoneyTransferRequest;
import com.nedbank.banking.dto.MoneyTransferResponse;
import com.nedbank.banking.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for money transfer operations between customer accounts
 */
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MoneyTransferController {

    private final TransactionService transactionService;

    /**
     * Transfer money from one account to another
     * 
     * POST /api/transfers/send
     * 
     * Required: CUSTOMER role (only customers can send money from their accounts)
     * 
     * @param request MoneyTransferRequest containing transfer details
     * @return MoneyTransferResponse with transaction details
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> transferMoney(@Valid @RequestBody MoneyTransferRequest request) {
        try {
            log.info("Received money transfer request from {} to {}", 
                    request.getFromAccountNumber(), request.getToAccountNumber());
            
            MoneyTransferResponse response = transactionService.transferMoney(request);
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            log.error("Security error during transfer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
                    
        } catch (IllegalArgumentException e) {
            log.error("Validation error during transfer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Unexpected error during transfer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("TRANSFER_FAILED", 
                            "An unexpected error occurred. Please try again later."));
        }
    }

    /**
     * Get transfer history for the current user
     * 
     * GET /api/transfers/history
     * 
     * @return List of transfer transactions (both sent and received)
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getTransferHistory() {
        try {
            // This can be implemented later to show transfer history
            // For now, users can use the transaction history endpoint
            return ResponseEntity.ok(createSuccessResponse(
                    "Transfer history available via /api/transactions/history/{accountNumber}"
            ));
        } catch (Exception e) {
            log.error("Error fetching transfer history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch transfer history"));
        }
    }

    /**
     * Validate account number before transfer (optional endpoint for UI)
     * 
     * GET /api/transfers/validate/{accountNumber}
     * 
     * @param accountNumber Account number to validate
     * @return Account holder name if valid
     */
    @GetMapping("/validate/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> validateAccount(@PathVariable String accountNumber) {
        try {
            log.info("Validating account number: {}", accountNumber);
            Map<String, Object> accountDetails = transactionService.validateAccountForTransfer(accountNumber);
            return ResponseEntity.ok(accountDetails);
        } catch (IllegalArgumentException e) {
            log.error("Account validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_ACCOUNT", e.getMessage()));
        } catch (Exception e) {
            log.error("Error validating account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to validate account"));
        }
    }

    // Helper methods
    
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", error);
        response.put("message", message);
        return response;
    }
    
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        return response;
    }
}

