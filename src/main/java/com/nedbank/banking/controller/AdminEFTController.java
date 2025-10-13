package com.nedbank.banking.controller;

import com.nedbank.banking.dto.NEFTBatchResponse;
import com.nedbank.banking.entity.EFTTransaction;
import com.nedbank.banking.repository.EFTTransactionRepository;
import com.nedbank.banking.service.BeneficiaryService;
import com.nedbank.banking.service.NEFTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller for monitoring EFT operations
 * Dashboard for NEFT batches, statistics, and transaction monitoring
 */
@RestController
@RequestMapping("/api/admin/eft")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminEFTController {

    private final NEFTService neftService;
    private final BeneficiaryService beneficiaryService;
    private final EFTTransactionRepository eftTransactionRepository;

    /**
     * Get NEFT dashboard overview
     * GET /api/admin/eft/dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<?> getDashboard() {
        try {
            log.debug("Fetching NEFT dashboard");

            // Calculate statistics
            long totalTransactions = eftTransactionRepository.countByEftType(
                    EFTTransaction.TYPE_NEFT);
            long pendingCount = eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_PENDING);
            long queuedCount = eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_QUEUED);
            long processingCount = eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_PROCESSING);
            long completedCount = eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_COMPLETED);
            long failedCount = eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_FAILED);

            // Calculate total amounts
            BigDecimal totalAmount = eftTransactionRepository.findByEftTypeAndStatusOrderByCreatedAtDesc(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_COMPLETED)
                    .stream()
                    .map(EFTTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCharges = eftTransactionRepository.findByEftTypeAndStatusOrderByCreatedAtDesc(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_COMPLETED)
                    .stream()
                    .map(EFTTransaction::getCharges)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Success rate
            double successRate = totalTransactions > 0 
                    ? (completedCount * 100.0) / totalTransactions 
                    : 0.0;

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("status", "success");
            dashboard.put("eftType", "NEFT");
            
            // Transaction counts
            Map<String, Object> counts = new HashMap<>();
            counts.put("total", totalTransactions);
            counts.put("pending", pendingCount);
            counts.put("queued", queuedCount);
            counts.put("processing", processingCount);
            counts.put("completed", completedCount);
            counts.put("failed", failedCount);
            dashboard.put("transactionCounts", counts);

            // Financial summary
            Map<String, Object> financial = new HashMap<>();
            financial.put("totalAmount", totalAmount);
            financial.put("totalCharges", totalCharges);
            financial.put("revenue", totalCharges);
            dashboard.put("financial", financial);

            // Statistics
            Map<String, Object> stats = new HashMap<>();
            stats.put("successRate", String.format("%.2f%%", successRate));
            stats.put("failureRate", String.format("%.2f%%", 100 - successRate));
            dashboard.put("statistics", stats);

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("Error fetching NEFT dashboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch dashboard"));
        }
    }

    /**
     * Get all NEFT batches
     * GET /api/admin/eft/batches
     */
    @GetMapping("/batches")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBatches() {
        try {
            log.debug("Fetching all NEFT batches");
            List<NEFTBatchResponse.BatchSummary> batches = neftService.getAllBatches();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", batches.size());
            response.put("batches", batches);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching NEFT batches", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch batches"));
        }
    }

    /**
     * Get batch details by batch ID
     * GET /api/admin/eft/batches/{batchId}
     */
    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<?> getBatchDetails(@PathVariable String batchId) {
        try {
            log.debug("Fetching batch details: {}", batchId);
            NEFTBatchResponse batch = neftService.getBatchDetails(batchId);
            return ResponseEntity.ok(createSuccessResponse("Batch details retrieved", batch));

        } catch (IllegalArgumentException e) {
            log.warn("Batch not found: {}", batchId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));

        } catch (Exception e) {
            log.error("Error fetching batch details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch batch details"));
        }
    }

    /**
     * Get all pending NEFT transactions
     * GET /api/admin/eft/pending
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingTransactions() {
        try {
            log.debug("Fetching pending NEFT transactions");
            List<EFTTransaction> pending = eftTransactionRepository.findPendingNEFTTransactions();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", pending.size());
            response.put("transactions", pending);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching pending transactions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch pending transactions"));
        }
    }

    /**
     * Get all NEFT transactions with filters
     * GET /api/admin/eft/transactions?status=COMPLETED
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllTransactions(@RequestParam(required = false) String status) {
        try {
            log.debug("Fetching NEFT transactions with status: {}", status);

            List<EFTTransaction> transactions;
            if (status != null && !status.isBlank()) {
                transactions = eftTransactionRepository.findByEftTypeAndStatusOrderByCreatedAtDesc(
                        EFTTransaction.TYPE_NEFT, status);
            } else {
                transactions = eftTransactionRepository.findByEftTypeOrderByCreatedAtDesc(
                        EFTTransaction.TYPE_NEFT);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("filter", status != null ? status : "ALL");
            response.put("count", transactions.size());
            response.put("transactions", transactions);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching transactions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch transactions"));
        }
    }

    /**
     * Get NEFT transaction by reference (Admin view)
     * GET /api/admin/eft/transactions/{reference}
     */
    @GetMapping("/transactions/{reference}")
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<?> getTransactionByReference(@PathVariable String reference) {
        try {
            log.debug("Fetching NEFT transaction: {}", reference);
            EFTTransaction transaction = eftTransactionRepository.findByEftReference(reference)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + reference));

            return ResponseEntity.ok(createSuccessResponse("Transaction found", transaction));

        } catch (IllegalArgumentException e) {
            log.warn("Transaction not found: {}", reference);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));

        } catch (Exception e) {
            log.error("Error fetching transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch transaction"));
        }
    }

    /**
     * Get all beneficiaries (Admin view)
     * GET /api/admin/eft/beneficiaries
     */
    @GetMapping("/beneficiaries")
    @PreAuthorize("hasAuthority('ACCOUNT_READ')")
    public ResponseEntity<?> getAllBeneficiaries() {
        try {
            log.debug("Admin fetching all beneficiaries");
            var beneficiaries = beneficiaryService.getAllBeneficiariesAdmin();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", beneficiaries.size());
            response.put("beneficiaries", beneficiaries);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("Access denied for beneficiaries list");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Access denied"));

        } catch (Exception e) {
            log.error("Error fetching beneficiaries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch beneficiaries"));
        }
    }

    /**
     * Get pending beneficiaries awaiting approval
     * GET /api/admin/eft/beneficiaries/pending
     */
    @GetMapping("/beneficiaries/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> getPendingBeneficiaries() {
        try {
            log.debug("Admin fetching pending beneficiaries");
            var beneficiaries = beneficiaryService.getPendingBeneficiariesAdmin();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", beneficiaries.size());
            response.put("beneficiaries", beneficiaries);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("Access denied for pending beneficiaries list");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Access denied"));

        } catch (Exception e) {
            log.error("Error fetching pending beneficiaries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch pending beneficiaries"));
        }
    }

    /**
     * Approve beneficiary
     * POST /api/admin/eft/beneficiaries/{id}/approve
     */
    @PostMapping("/beneficiaries/{id}/approve")
    @PreAuthorize("hasAuthority('ACCOUNT_WRITE')")
    public ResponseEntity<?> approveBeneficiary(@PathVariable Long id) {
        try {
            log.info("Admin approving beneficiary: {}", id);
            var beneficiary = beneficiaryService.approveBeneficiary(id);
            return ResponseEntity.ok(createSuccessResponse("Beneficiary approved successfully", beneficiary));

        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));

        } catch (SecurityException e) {
            log.warn("Access denied for beneficiary approval");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Access denied"));

        } catch (Exception e) {
            log.error("Error approving beneficiary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to approve beneficiary"));
        }
    }

    /**
     * Reject beneficiary
     * POST /api/admin/eft/beneficiaries/{id}/reject
     */
    @PostMapping("/beneficiaries/{id}/reject")
    @PreAuthorize("hasAuthority('ACCOUNT_WRITE')")
    public ResponseEntity<?> rejectBeneficiary(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            String reason = requestBody != null ? requestBody.getOrDefault("reason", "Rejected by admin") : "Rejected by admin";
            log.info("Admin rejecting beneficiary: {}", id);
            
            beneficiaryService.rejectBeneficiary(id, reason);
            return ResponseEntity.ok(createSuccessResponse("Beneficiary rejected successfully", null));

        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));

        } catch (SecurityException e) {
            log.warn("Access denied for beneficiary rejection");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Access denied"));

        } catch (Exception e) {
            log.error("Error rejecting beneficiary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to reject beneficiary"));
        }
    }

    /**
     * Block beneficiary
     * POST /api/admin/eft/beneficiaries/{id}/block
     */
    @PostMapping("/beneficiaries/{id}/block")
    @PreAuthorize("hasAuthority('ACCOUNT_WRITE')")
    public ResponseEntity<?> blockBeneficiary(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            String reason = requestBody != null ? requestBody.getOrDefault("reason", "Blocked by admin") : "Blocked by admin";
            log.info("Admin blocking beneficiary: {}", id);
            
            beneficiaryService.blockBeneficiary(id, reason);
            return ResponseEntity.ok(createSuccessResponse("Beneficiary blocked successfully", null));

        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));

        } catch (SecurityException e) {
            log.warn("Access denied for beneficiary blocking");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Access denied"));

        } catch (Exception e) {
            log.error("Error blocking beneficiary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to block beneficiary"));
        }
    }

    /**
     * Get NEFT statistics summary
     * GET /api/admin/eft/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public ResponseEntity<?> getStatistics() {
        try {
            log.debug("Fetching NEFT statistics");

            // Get counts by status
            Map<String, Long> statusCounts = new HashMap<>();
            statusCounts.put("PENDING", eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_PENDING));
            statusCounts.put("QUEUED", eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_QUEUED));
            statusCounts.put("PROCESSING", eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_PROCESSING));
            statusCounts.put("COMPLETED", eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_COMPLETED));
            statusCounts.put("FAILED", eftTransactionRepository.countByEftTypeAndStatus(
                    EFTTransaction.TYPE_NEFT, EFTTransaction.STATUS_FAILED));

            // Get batch count
            long batchCount = eftTransactionRepository.findDistinctBatchIds().size();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("status", "success");
            statistics.put("eftType", "NEFT");
            statistics.put("statusCounts", statusCounts);
            statistics.put("totalBatches", batchCount);

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("Error fetching statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to fetch statistics"));
        }
    }

    /**
     * Manually trigger batch processing (for testing)
     * POST /api/admin/eft/process-batch
     */
    @PostMapping("/process-batch")
    @PreAuthorize("hasAuthority('TRANSACTION_WRITE')")
    public ResponseEntity<?> triggerBatchProcessing() {
        try {
            log.info("Admin manually triggering NEFT batch processing");
            neftService.processBatch();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Batch processing triggered successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error triggering batch processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("ERROR", "Failed to trigger batch processing"));
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

