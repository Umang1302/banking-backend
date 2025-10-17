package com.nedbank.banking.controller;

import com.nedbank.banking.dto.*;
import com.nedbank.banking.service.StatementService;
import com.nedbank.banking.service.TransactionService;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final StatementService statementService;

    /**
     * Create a new transaction (debit/credit/transfer)
     * Requires TRANSACTION_WRITE permission
     */
    @Transactional
    @PostMapping
    @PreAuthorize("hasAuthority('TRANSACTION_WRITE')")
    public ResponseEntity<?> createTransaction(@Valid @RequestBody TransactionRequest request) {
        try {
            log.debug("Creating transaction: {}", request.getTransactionType());
            TransactionResponse response = transactionService.createTransaction(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Transaction completed successfully");
            result.put("transaction", response);
            result.put("status", "success");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Transaction validation error: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            log.warn("Transaction state error: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), "TRANSACTION_FAILED", HttpStatus.CONFLICT);
        } catch (SecurityException e) {
            log.warn("Access denied for transaction: {}", e.getMessage());
            return buildErrorResponse("Access denied", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Error creating transaction: {}", e.getMessage());
            return buildErrorResponse("Failed to process transaction", "INTERNAL_ERROR", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get transaction history for an account
     * Customer can view own account, admin/accountant can view any
     */
    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<?> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            log.debug("Retrieving transaction history for account: {}", accountNumber);
            List<TransactionResponse> transactions = transactionService.getTransactionHistory(
                    accountNumber, startDate, endDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("accountNumber", accountNumber);
            response.put("transactions", transactions);
            response.put("count", transactions.size());
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid account: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), "ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            log.warn("Access denied for transaction history: {}", e.getMessage());
            return buildErrorResponse("Access denied", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Error retrieving transaction history: {}", e.getMessage());
            return buildErrorResponse("Failed to retrieve transactions", "INTERNAL_ERROR", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get transaction by reference number
     */
    @GetMapping("/reference/{reference}")
    public ResponseEntity<?> getTransactionByReference(@PathVariable String reference) {
        try {
            log.debug("Retrieving transaction by reference: {}", reference);
            TransactionResponse transaction = transactionService.getTransactionByReference(reference);
            return ResponseEntity.ok(transaction);
        } catch (IllegalArgumentException e) {
            log.warn("Transaction not found: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), "TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            log.warn("Access denied for transaction: {}", e.getMessage());
            return buildErrorResponse("Access denied", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Error retrieving transaction: {}", e.getMessage());
            return buildErrorResponse("Failed to retrieve transaction", "INTERNAL_ERROR", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Bulk upload transactions from CSV file
     * Requires TRANSACTION_WRITE permission (Accountants/Admins)
     */
    @Transactional
    @PostMapping("/bulk-upload")
    @PreAuthorize("hasAuthority('TRANSACTION_WRITE')")
    public ResponseEntity<?> bulkUploadTransactions(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return buildErrorResponse("File is empty", "EMPTY_FILE", HttpStatus.BAD_REQUEST);
            }
            
            if (!file.getOriginalFilename().endsWith(".csv")) {
                return buildErrorResponse("Only CSV files are supported", "INVALID_FILE_TYPE", 
                        HttpStatus.BAD_REQUEST);
            }
            
            log.info("Processing bulk transaction upload: {}", file.getOriginalFilename());
            BulkTransactionResponse response = transactionService.bulkUploadTransactions(file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Bulk upload processed");
            result.put("result", response);
            result.put("status", "success");
            
            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            log.warn("Access denied for bulk upload: {}", e.getMessage());
            return buildErrorResponse("Access denied", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Error processing bulk upload: {}", e.getMessage());
            return buildErrorResponse("Failed to process bulk upload", "INTERNAL_ERROR", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate and download PDF bank statement
     */
    @PostMapping("/statement/pdf")
    public ResponseEntity<?> generatePdfStatement(@Valid @RequestBody StatementRequest request) {
        try {
            log.debug("Generating PDF statement for account: {}", request.getAccountNumber());
            byte[] pdfBytes = statementService.generatePdfStatement(request);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                    "statement_" + request.getAccountNumber() + "_" + 
                    request.getStartDate() + "_to_" + request.getEndDate() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid statement request: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            log.warn("Access denied for statement generation: {}", e.getMessage());
            return buildErrorResponse("Access denied", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Error generating statement: {}", e.getMessage());
            return buildErrorResponse("Failed to generate statement", "INTERNAL_ERROR", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all transactions (Admin/Accountant only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<?> getAllTransactions() {
        try {
            log.debug("Retrieving all transactions");
            List<TransactionResponse> transactions = transactionService.getAllTransactions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions);
            response.put("count", transactions.size());
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("Access denied for all transactions: {}", e.getMessage());
            return buildErrorResponse("Access denied", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Error retrieving all transactions: {}", e.getMessage());
            return buildErrorResponse("Failed to retrieve transactions", "INTERNAL_ERROR", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get pending transactions (Admin/Accountant only)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<?> getPendingTransactions() {
        try {
            log.debug("Retrieving pending transactions");
            List<TransactionResponse> transactions = transactionService.getPendingTransactions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions);
            response.put("count", transactions.size());
            response.put("status", "PENDING");
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("Access denied for pending transactions: {}", e.getMessage());
            return buildErrorResponse("Access denied", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Error retrieving pending transactions: {}", e.getMessage());
            return buildErrorResponse("Failed to retrieve transactions", "INTERNAL_ERROR", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Helper method to build error responses
     */
    private ResponseEntity<?> buildErrorResponse(String message, String code, HttpStatus status) {
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        error.put("code", code);
        error.put("status", "error");
        return ResponseEntity.status(status).body(error);
    }
}

