package com.nedbank.banking.controller;

import com.nedbank.banking.dto.*;
import com.nedbank.banking.service.QRPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for QR Payment operations
 * Handles QR code generation, parsing, and payment processing
 */
@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QRPaymentController {

    private final QRPaymentService qrPaymentService;

    /**
     * Generate QR code for receiving payment
     * POST /api/qr/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> generateQRPayment(@Valid @RequestBody QRGenerateRequest request) {
        try {
            log.info("Received QR generation request for account: {}, amount: {}", 
                    request.getAccountNumber(), request.getAmount());
            
            QRGenerateResponse response = qrPaymentService.generateQRPayment(request);
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.error("Security error generating QR: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Security Error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Validation error generating QR: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation Error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating QR payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to generate QR code"));
        }
    }

    /**
     * Get QR payment request details by ID
     * GET /api/qr/{requestId}
     */
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> getQRPaymentRequest(@PathVariable String requestId) {
        try {
            log.info("Fetching QR payment request: {}", requestId);
            
            QRGenerateResponse response = qrPaymentService.getQRPaymentRequest(requestId);
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.error("Security error fetching QR request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Security Error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("QR request not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching QR request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to fetch QR request"));
        }
    }

    /**
     * Get all QR payment requests for current user
     * GET /api/qr/my-requests
     */
    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> getMyQRPaymentRequests() {
        try {
            log.info("Fetching QR payment requests for current user");
            
            List<QRGenerateResponse> responses = qrPaymentService.getMyQRPaymentRequests();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error fetching QR requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to fetch QR requests"));
        }
    }

    /**
     * Parse/decode uploaded QR code
     * POST /api/qr/parse
     */
    @PostMapping("/parse")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> parseQRCode(@Valid @RequestBody QRParseRequest request) {
        try {
            log.info("Parsing QR code");
            
            QRParseResponse response = qrPaymentService.parseQRCode(request);
            
            if (!response.isValid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error parsing QR code: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(QRParseResponse.invalid("Failed to parse QR code"));
        }
    }

    /**
     * Process QR payment (internal transfer)
     * POST /api/qr/pay
     */
    @PostMapping("/pay")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> processQRPayment(@Valid @RequestBody QRPayRequest request) {
        try {
            log.info("Processing QR payment for request: {}", request.getRequestId());
            
            QRPayResponse response = qrPaymentService.processQRPayment(request);
            
            if (!response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.error("Security error processing QR payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(QRPayResponse.failure("Security Error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Validation error processing QR payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(QRPayResponse.failure("Validation Error: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing QR payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(QRPayResponse.failure("Payment processing failed"));
        }
    }

    /**
     * Get QR transaction history for an account
     * GET /api/qr/transactions/{accountNumber}
     */
    @GetMapping("/transactions/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<?> getQRTransactionHistory(@PathVariable String accountNumber) {
        try {
            log.info("Fetching QR transaction history for account: {}", accountNumber);
            
            List<QRTransactionResponse> responses = qrPaymentService.getQRTransactionHistory(accountNumber);
            
            return ResponseEntity.ok(responses);
        } catch (SecurityException e) {
            log.error("Security error fetching QR transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Security Error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Account not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching QR transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server Error", "Failed to fetch QR transactions"));
        }
    }

    /**
     * Error response DTO
     */
    private record ErrorResponse(String error, String message) {}
}

