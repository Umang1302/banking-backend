package com.nedbank.banking.controller;

import com.nedbank.banking.dto.*;
import com.nedbank.banking.service.BeneficiaryService;
import com.nedbank.banking.service.NEFTService;
import com.nedbank.banking.util.IFSCValidator;
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

/**
 * Controller for EFT operations - Beneficiary management and NEFT transfers
 */
@RestController
@RequestMapping("/api/eft")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EFTController {

    private final BeneficiaryService beneficiaryService;
    private final NEFTService neftService;

    // ==================== Beneficiary Management ====================

    /**
     * Add a new beneficiary
     * POST /api/eft/beneficiaries
     */
    @PostMapping("/beneficiaries")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> addBeneficiary(@Valid @RequestBody BeneficiaryRequest request) {
        try {
            log.info("Adding new beneficiary: {}", request.getBeneficiaryName());
            BeneficiaryResponse response = beneficiaryService.addBeneficiary(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createSuccessResponse("Beneficiary added successfully", response));
                    
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error adding beneficiary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to add beneficiary"));
        }
    }

    /**
     * Get all beneficiaries
     * GET /api/eft/beneficiaries
     */
    @GetMapping("/beneficiaries")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getAllBeneficiaries() {
        try {
            log.debug("Fetching all beneficiaries");
            List<BeneficiaryResponse> beneficiaries = beneficiaryService.getAllBeneficiaries();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", beneficiaries.size());
            response.put("beneficiaries", beneficiaries);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching beneficiaries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch beneficiaries"));
        }
    }

    /**
     * Get active beneficiaries only
     * GET /api/eft/beneficiaries/active
     */
    @GetMapping("/beneficiaries/active")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getActiveBeneficiaries() {
        try {
            log.debug("Fetching active beneficiaries");
            List<BeneficiaryResponse> beneficiaries = beneficiaryService.getActiveBeneficiaries();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", beneficiaries.size());
            response.put("beneficiaries", beneficiaries);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching active beneficiaries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch beneficiaries"));
        }
    }

    /**
     * Get beneficiary by ID
     * GET /api/eft/beneficiaries/{id}
     */
    @GetMapping("/beneficiaries/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getBeneficiaryById(@PathVariable Long id) {
        try {
            log.debug("Fetching beneficiary: {}", id);
            BeneficiaryResponse beneficiary = beneficiaryService.getBeneficiaryById(id);
            return ResponseEntity.ok(createSuccessResponse("Beneficiary found", beneficiary));
            
        } catch (IllegalArgumentException e) {
            log.warn("Beneficiary not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error fetching beneficiary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch beneficiary"));
        }
    }

    /**
     * Update beneficiary
     * PUT /api/eft/beneficiaries/{id}
     */
    @PutMapping("/beneficiaries/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateBeneficiary(
            @PathVariable Long id,
            @Valid @RequestBody BeneficiaryRequest request) {
        try {
            log.info("Updating beneficiary: {}", id);
            BeneficiaryResponse response = beneficiaryService.updateBeneficiary(id, request);
            return ResponseEntity.ok(createSuccessResponse("Beneficiary updated successfully", response));
            
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error updating beneficiary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to update beneficiary"));
        }
    }

    /**
     * Delete beneficiary
     * DELETE /api/eft/beneficiaries/{id}
     */
    @DeleteMapping("/beneficiaries/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> deleteBeneficiary(@PathVariable Long id) {
        try {
            log.info("Deleting beneficiary: {}", id);
            beneficiaryService.deleteBeneficiary(id);
            return ResponseEntity.ok(createSuccessResponse("Beneficiary deleted successfully", null));
            
        } catch (IllegalArgumentException e) {
            log.warn("Beneficiary not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error deleting beneficiary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to delete beneficiary"));
        }
    }

    /**
     * Validate IFSC code and get bank details
     * GET /api/eft/validate-ifsc/{ifscCode}
     */
    @GetMapping("/validate-ifsc/{ifscCode}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> validateIfsc(@PathVariable String ifscCode) {
        try {
            log.debug("Validating IFSC code: {}", ifscCode);
            
            // Validate and get bank details
            IFSCValidator.IFSCDetails details = IFSCValidator.validateAndGetDetails(ifscCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "IFSC code is valid");
            response.put("ifscCode", details.getIfscCode());
            response.put("bankCode", details.getBankCode());
            response.put("branchCode", details.getBranchCode());
            response.put("bankName", details.getBankName());
            response.put("isValid", details.isValid());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("IFSC validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_IFSC", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error validating IFSC code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to validate IFSC code"));
        }
    }

    // ==================== NEFT Transfer Operations ====================

    /**
     * Initiate NEFT transfer
     * POST /api/eft/neft/transfer
     */
    @PostMapping("/transfer/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> initiateNEFTTransfer(@Valid @RequestBody NEFTTransferRequest request) {
        try {
            log.info("Initiating NEFT transfer from {} to beneficiary {}", 
                    request.getFromAccountNumber(), request.getBeneficiaryId());
            
            NEFTTransferResponse response = neftService.initiateNEFTTransfer(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createSuccessResponse("NEFT transfer initiated successfully", response));
                    
        } catch (SecurityException e) {
            log.error("Security error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
                    
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error initiating NEFT transfer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("TRANSFER_FAILED", 
                            "Failed to initiate NEFT transfer. Please try again later."));
        }
    }

    /**
     * Get NEFT transaction status
     * GET /api/eft/neft/status/{reference}
     */
    @GetMapping("/neft/status/{reference}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getNEFTStatus(@PathVariable String reference) {
        try {
            log.debug("Fetching NEFT status: {}", reference);
            EFTStatusResponse status = neftService.getNEFTStatus(reference);
            return ResponseEntity.ok(createSuccessResponse("NEFT status retrieved", status));
            
        } catch (IllegalArgumentException e) {
            log.warn("NEFT transaction not found: {}", reference);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
                    
        } catch (SecurityException e) {
            log.warn("Access denied for NEFT status: {}", reference);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Access denied"));
                    
        } catch (Exception e) {
            log.error("Error fetching NEFT status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch NEFT status"));
        }
    }

    /**
     * Get NEFT history for account
     * GET /api/eft/neft/history/{accountNumber}
     */
    @GetMapping("/neft/history/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getNEFTHistory(@PathVariable String accountNumber) {
        try {
            log.debug("Fetching NEFT history for account: {}", accountNumber);
            List<EFTStatusResponse> history = neftService.getNEFTHistory(accountNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("accountNumber", accountNumber);
            response.put("count", history.size());
            response.put("transactions", history);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Account not found: {}", accountNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
                    
        } catch (SecurityException e) {
            log.warn("Access denied for NEFT history: {}", accountNumber);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Access denied"));
                    
        } catch (Exception e) {
            log.error("Error fetching NEFT history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch NEFT history"));
        }
    }

    // Helper methods

    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }

    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}

