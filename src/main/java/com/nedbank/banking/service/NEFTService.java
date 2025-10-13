package com.nedbank.banking.service;

import com.nedbank.banking.dto.*;
import com.nedbank.banking.entity.*;
import com.nedbank.banking.repository.AccountRepository;
import com.nedbank.banking.repository.EFTTransactionRepository;
import com.nedbank.banking.repository.TransactionRepository;
import com.nedbank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for NEFT (National Electronic Funds Transfer) operations
 * Handles NEFT transfers with batch processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NEFTService {

    private final EFTTransactionRepository eftTransactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BeneficiaryService beneficiaryService;

    // NEFT charges (can be configurable)
    private static final BigDecimal NEFT_CHARGE_UPTO_10K = new BigDecimal("2.50");
    private static final BigDecimal NEFT_CHARGE_10K_TO_1L = new BigDecimal("5.00");
    private static final BigDecimal NEFT_CHARGE_1L_TO_2L = new BigDecimal("15.00");
    private static final BigDecimal NEFT_CHARGE_ABOVE_2L = new BigDecimal("25.00");

    // NEFT operates in hourly batches from 8 AM to 7 PM (weekdays)
    private static final LocalTime FIRST_BATCH = LocalTime.of(8, 0);
    private static final LocalTime LAST_BATCH = LocalTime.of(19, 0);

    /**
     * Initiate NEFT transfer
     */
    @Transactional
    public NEFTTransferResponse initiateNEFTTransfer(NEFTTransferRequest request) {
        User currentUser = getCurrentUser();
        log.info("User {} initiating NEFT transfer", currentUser.getUsername());

        // Validate source account
        Account sourceAccount = accountRepository.findByAccountNumberWithCustomer(request.getFromAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));

        // Security check - user can only transfer from their own account
        if (!hasAccessToAccount(currentUser, sourceAccount)) {
            throw new SecurityException("You are not authorized to transfer from this account");
        }

        // Validate account status
        if (!"ACTIVE".equals(sourceAccount.getStatus())) {
            throw new IllegalArgumentException("Source account is not active");
        }

        // Validate and get beneficiary
        Beneficiary beneficiary = beneficiaryService.validateBeneficiaryForTransfer(
                request.getBeneficiaryId(), 
                currentUser.getCustomer().getId()
        );

        // Calculate charges
        BigDecimal charges = calculateNEFTCharges(request.getAmount());
        BigDecimal totalAmount = request.getAmount().add(charges);

        // Validate sufficient balance
        if (sourceAccount.getAvailableBalance().compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + 
                    sourceAccount.getAvailableBalance() + ", Required: " + totalAmount);
        }

        // Validate minimum balance requirement
        BigDecimal balanceAfterTransfer = sourceAccount.getBalance().subtract(totalAmount);
        if (balanceAfterTransfer.compareTo(sourceAccount.getMinimumBalance()) < 0) {
            throw new IllegalArgumentException("Transfer would violate minimum balance requirement of " + 
                    sourceAccount.getMinimumBalance());
        }

        // Calculate next batch time and estimated completion
        LocalTime now = LocalTime.now();
        LocalTime nextBatchTime = getNextBatchTime(now);
        LocalDateTime estimatedCompletion = LocalDateTime.of(LocalDate.now(), nextBatchTime).plusMinutes(30);

        // Debit from source account immediately
        BigDecimal balanceBefore = sourceAccount.getBalance();
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(totalAmount));
        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(totalAmount));
        sourceAccount.setLastTransactionDate(LocalDateTime.now());
        accountRepository.save(sourceAccount);

        // Create internal transaction record (for debit)
        // Status is PROCESSING until batch completes
        Transaction transaction = Transaction.builder()
                .account(sourceAccount)
                .transactionType(Transaction.TYPE_DEBIT)
                .amount(totalAmount)
                .currency(sourceAccount.getCurrency())
                .balanceBefore(balanceBefore)
                .balanceAfter(sourceAccount.getBalance())
                .description("NEFT Transfer to " + beneficiary.getBeneficiaryName() + " - " + request.getPurpose())
                .category(Transaction.CATEGORY_TRANSFER)
                .status(Transaction.STATUS_PROCESSING)
                .transactionDate(LocalDateTime.now())
                .valueDate(LocalDateTime.now())
                .initiatedBy(currentUser.getUsername())
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Create EFT transaction record
        EFTTransaction eftTransaction = EFTTransaction.builder()
                .eftType(EFTTransaction.TYPE_NEFT)
                .sourceAccount(sourceAccount)
                .beneficiary(beneficiary)
                .beneficiaryAccountNumber(beneficiary.getAccountNumber())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .beneficiaryIfsc(beneficiary.getIfscCode())
                .beneficiaryBankName(beneficiary.getBankName())
                .amount(request.getAmount())
                .charges(charges)
                .totalAmount(totalAmount)
                .currency(sourceAccount.getCurrency())
                .purpose(request.getPurpose())
                .remarks(request.getRemarks())
                .status(EFTTransaction.STATUS_PENDING)
                .batchTime(nextBatchTime)
                .estimatedCompletion(estimatedCompletion)
                .initiatedBy(currentUser.getUsername())
                .transaction(savedTransaction)
                .build();

        EFTTransaction savedEFT = eftTransactionRepository.save(eftTransaction);

        log.info("NEFT transfer initiated. Reference: {}, Amount: {}, Next batch: {}", 
                savedEFT.getEftReference(), request.getAmount(), nextBatchTime);

        return NEFTTransferResponse.success(
                savedEFT.getId(),
                savedEFT.getEftReference(),
                sourceAccount.getAccountNumber(),
                beneficiary.getAccountNumber(),
                beneficiary.getBeneficiaryName(),
                beneficiary.getBankName(),
                beneficiary.getIfscCode(),
                request.getAmount(),
                charges,
                request.getPurpose(),
                nextBatchTime,
                estimatedCompletion,
                savedEFT.getCreatedAt()
        );
    }

    /**
     * Get NEFT transaction status
     */
    @Transactional(readOnly = true)
    public EFTStatusResponse getNEFTStatus(String eftReference) {
        User currentUser = getCurrentUser();

        EFTTransaction eft = eftTransactionRepository.findByEftReference(eftReference)
                .orElseThrow(() -> new IllegalArgumentException("NEFT transaction not found: " + eftReference));

        // Security check
        if (!hasAccessToAccount(currentUser, eft.getSourceAccount())) {
            throw new SecurityException("Access denied");
        }

        return mapToStatusResponse(eft);
    }

    /**
     * Get NEFT history for user's accounts
     */
    @Transactional(readOnly = true)
    public List<EFTStatusResponse> getNEFTHistory(String accountNumber) {
        User currentUser = getCurrentUser();

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!hasAccessToAccount(currentUser, account)) {
            throw new SecurityException("Access denied");
        }

        List<EFTTransaction> transactions = eftTransactionRepository
                .findBySourceAccountIdOrderByCreatedAtDesc(account.getId());

        return transactions.stream()
                .filter(EFTTransaction::isNEFT)
                .map(this::mapToStatusResponse)
                .collect(Collectors.toList());
    }

    /**
     * Scheduled batch processor - runs every hour
     * Processes all pending NEFT transactions
     */
    @Scheduled(cron = "0 0 * * * *") // Run at the start of every hour
    @Transactional
    public void processBatch() {
        LocalTime currentTime = LocalTime.now();
        
        // Check if within operating hours
        if (currentTime.isBefore(FIRST_BATCH) || currentTime.isAfter(LAST_BATCH)) {
            log.debug("NEFT batch processing outside operating hours. Current time: {}", currentTime);
            return;
        }

        log.info("Starting NEFT batch processing at {}", currentTime);

        List<EFTTransaction> pendingTransactions = eftTransactionRepository.findPendingNEFTTransactions();

        if (pendingTransactions.isEmpty()) {
            log.info("No pending NEFT transactions to process");
            return;
        }

        // Generate batch ID
        String batchId = generateBatchId();
        int successCount = 0;
        int failCount = 0;

        log.info("Processing NEFT batch {} with {} transactions", batchId, pendingTransactions.size());

        for (EFTTransaction eft : pendingTransactions) {
            try {
                // Update status to QUEUED
                eft.setStatus(EFTTransaction.STATUS_QUEUED);
                eft.setBatchId(batchId);
                eftTransactionRepository.save(eft);

                // Simulate processing (in real scenario, this would call external payment gateway)
                processNEFTTransaction(eft);

                // Update EFT status to COMPLETED
                eft.setStatus(EFTTransaction.STATUS_COMPLETED);
                eft.setActualCompletion(LocalDateTime.now());
                eft.setProcessedBy("NEFT_BATCH_PROCESSOR");
                eftTransactionRepository.save(eft);

                // Update internal transaction status to COMPLETED
                Transaction internalTxn = eft.getTransaction();
                if (internalTxn != null) {
                    internalTxn.setStatus(Transaction.STATUS_COMPLETED);
                    internalTxn.setApprovedBy("NEFT_BATCH_PROCESSOR");
                    internalTxn.setApprovalDate(LocalDateTime.now());
                    transactionRepository.save(internalTxn);
                }

                successCount++;
                log.debug("NEFT transaction processed successfully: {}", eft.getEftReference());

            } catch (Exception e) {
                // Mark EFT as failed
                eft.setStatus(EFTTransaction.STATUS_FAILED);
                eft.setFailureReason(e.getMessage());
                eft.setActualCompletion(LocalDateTime.now());
                eftTransactionRepository.save(eft);

                // Mark internal transaction as FAILED
                Transaction internalTxn = eft.getTransaction();
                if (internalTxn != null) {
                    internalTxn.setStatus(Transaction.STATUS_FAILED);
                    internalTxn.setFailureReason(e.getMessage());
                    transactionRepository.save(internalTxn);
                }

                // Refund amount to source account
                refundFailedTransfer(eft);

                failCount++;
                log.error("NEFT transaction failed: {}. Error: {}", eft.getEftReference(), e.getMessage());
            }
        }

        log.info("NEFT batch {} processing completed. Success: {}, Failed: {}", batchId, successCount, failCount);
    }

    /**
     * Process individual NEFT transaction (simulated)
     */
    private void processNEFTTransaction(EFTTransaction eft) {
        // Update to PROCESSING
        eft.setStatus(EFTTransaction.STATUS_PROCESSING);
        eftTransactionRepository.save(eft);

        // Simulate external bank transfer
        // In production, this would integrate with payment gateway/NEFT network
        log.info("Simulating NEFT transfer to {} for amount {}", 
                eft.getBeneficiaryBankName(), eft.getAmount());

        // Simulate processing delay (remove in production)
        try {
            Thread.sleep(1000); // 1 second simulation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Random failure simulation (5% failure rate for demo)
        if (Math.random() < 0.05) {
            throw new RuntimeException("Simulated: Beneficiary account not found or inactive");
        }

        // Success - external bank has credited the beneficiary
        log.info("NEFT transfer successful: {}", eft.getEftReference());
    }

    /**
     * Refund amount if NEFT fails
     */
    private void refundFailedTransfer(EFTTransaction eft) {
        try {
            Account sourceAccount = eft.getSourceAccount();
            BigDecimal refundAmount = eft.getTotalAmount();

            // Credit back to source account
            sourceAccount.setBalance(sourceAccount.getBalance().add(refundAmount));
            sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().add(refundAmount));
            accountRepository.save(sourceAccount);

            // Create refund transaction
            Transaction refundTransaction = Transaction.builder()
                    .account(sourceAccount)
                    .transactionType(Transaction.TYPE_CREDIT)
                    .amount(refundAmount)
                    .currency(sourceAccount.getCurrency())
                    .balanceBefore(sourceAccount.getBalance().subtract(refundAmount))
                    .balanceAfter(sourceAccount.getBalance())
                    .description("Refund - NEFT transfer failed: " + eft.getEftReference())
                    .category(Transaction.CATEGORY_REFUND)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(LocalDateTime.now())
                    .initiatedBy("SYSTEM")
                    .approvedBy("SYSTEM")
                    .approvalDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(refundTransaction);

            log.info("Refund processed for failed NEFT: {}", eft.getEftReference());
        } catch (Exception e) {
            log.error("Error processing refund for failed NEFT: {}", eft.getEftReference(), e);
        }
    }

    /**
     * Get batch details (Admin)
     */
    @Transactional(readOnly = true)
    public NEFTBatchResponse getBatchDetails(String batchId) {
        List<EFTTransaction> transactions = eftTransactionRepository.findByBatchIdOrderByCreatedAtAsc(batchId);

        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Batch not found: " + batchId);
        }

        int successCount = (int) transactions.stream().filter(EFTTransaction::isCompleted).count();
        int failCount = (int) transactions.stream().filter(EFTTransaction::isFailed).count();
        BigDecimal totalAmount = transactions.stream()
                .map(EFTTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        EFTTransaction firstTxn = transactions.get(0);

        return NEFTBatchResponse.builder()
                .batchId(batchId)
                .batchTime(firstTxn.getBatchTime())
                .processedAt(firstTxn.getActualCompletion())
                .totalTransactions(transactions.size())
                .successfulTransactions(successCount)
                .failedTransactions(failCount)
                .totalAmount(totalAmount)
                .status(failCount > 0 ? "PARTIALLY_COMPLETED" : "COMPLETED")
                .transactions(transactions.stream().map(this::mapToStatusResponse).collect(Collectors.toList()))
                .build();
    }

    /**
     * Get all batches summary (Admin)
     */
    @Transactional(readOnly = true)
    public List<NEFTBatchResponse.BatchSummary> getAllBatches() {
        List<String> batchIds = eftTransactionRepository.findDistinctBatchIds();

        return batchIds.stream().map(batchId -> {
            List<EFTTransaction> transactions = eftTransactionRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
            
            BigDecimal totalAmount = transactions.stream()
                    .map(EFTTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            EFTTransaction firstTxn = transactions.get(0);
            int failCount = (int) transactions.stream().filter(EFTTransaction::isFailed).count();

            return NEFTBatchResponse.BatchSummary.builder()
                    .batchId(batchId)
                    .batchTime(firstTxn.getBatchTime())
                    .transactionCount(transactions.size())
                    .totalAmount(totalAmount)
                    .status(failCount > 0 ? "PARTIALLY_COMPLETED" : "COMPLETED")
                    .processedAt(firstTxn.getActualCompletion())
                    .build();
        }).collect(Collectors.toList());
    }

    // Helper methods

    /**
     * Calculate NEFT charges based on amount
     */
    private BigDecimal calculateNEFTCharges(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("10000")) <= 0) {
            return NEFT_CHARGE_UPTO_10K;
        } else if (amount.compareTo(new BigDecimal("100000")) <= 0) {
            return NEFT_CHARGE_10K_TO_1L;
        } else if (amount.compareTo(new BigDecimal("200000")) <= 0) {
            return NEFT_CHARGE_1L_TO_2L;
        } else {
            return NEFT_CHARGE_ABOVE_2L;
        }
    }

    /**
     * Get next NEFT batch time
     */
    private LocalTime getNextBatchTime(LocalTime currentTime) {
        if (currentTime.isBefore(FIRST_BATCH)) {
            return FIRST_BATCH;
        }
        
        if (currentTime.isAfter(LAST_BATCH)) {
            // Next day's first batch
            return FIRST_BATCH;
        }

        // Next hour
        int nextHour = currentTime.getHour() + 1;
        return LocalTime.of(nextHour, 0);
    }

    /**
     * Generate unique batch ID
     */
    private String generateBatchId() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("NEFT%04d%02d%02d%02d", 
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found: " + username));
    }

    private boolean hasAccessToAccount(User user, Account account) {
        if (user.hasPermission("ACCOUNT_READ")) {
            return true;
        }
        if (user.getCustomer() != null) {
            return account.getCustomer().getId().equals(user.getCustomer().getId());
        }
        return false;
    }

    private EFTStatusResponse mapToStatusResponse(EFTTransaction eft) {
        return EFTStatusResponse.builder()
                .eftTransactionId(eft.getId())
                .eftReference(eft.getEftReference())
                .eftType(eft.getEftType())
                .status(eft.getStatus())
                .fromAccountNumber(eft.getSourceAccount().getAccountNumber())
                .beneficiaryAccountNumber(eft.getBeneficiaryAccountNumber())
                .beneficiaryName(eft.getBeneficiaryName())
                .beneficiaryBank(eft.getBeneficiaryBankName())
                .amount(eft.getAmount())
                .charges(eft.getCharges())
                .totalAmount(eft.getTotalAmount())
                .currency(eft.getCurrency())
                .purpose(eft.getPurpose())
                .batchId(eft.getBatchId())
                .batchTime(eft.getBatchTime())
                .estimatedCompletion(eft.getEstimatedCompletion())
                .actualCompletion(eft.getActualCompletion())
                .failureReason(eft.getFailureReason())
                .initiatedAt(eft.getCreatedAt())
                .updatedAt(eft.getUpdatedAt())
                .build();
    }
}

