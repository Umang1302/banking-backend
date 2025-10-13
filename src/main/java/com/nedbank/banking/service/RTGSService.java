package com.nedbank.banking.service;

import com.nedbank.banking.dto.*;
import com.nedbank.banking.entity.*;
import com.nedbank.banking.repository.AccountRepository;
import com.nedbank.banking.repository.EFTTransactionRepository;
import com.nedbank.banking.repository.TransactionRepository;
import com.nedbank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for RTGS (Real Time Gross Settlement) operations
 * Handles high-value RTGS transfers with real-time processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RTGSService {

    private final EFTTransactionRepository eftTransactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BeneficiaryService beneficiaryService;

    // RTGS minimum and charges
    private static final BigDecimal RTGS_MINIMUM_AMOUNT = new BigDecimal("200000");
    private static final BigDecimal RTGS_CHARGE_2L_TO_5L = new BigDecimal("30.00");
    private static final BigDecimal RTGS_CHARGE_ABOVE_5L = new BigDecimal("55.00");

    // RTGS operates from 9 AM to 4:30 PM (weekdays)
    private static final LocalTime RTGS_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime RTGS_END_TIME = LocalTime.of(16, 30);

    /**
     * Initiate RTGS transfer with real-time processing
     */
    @Transactional
    public RTGSTransferResponse initiateRTGSTransfer(RTGSTransferRequest request) {
        User currentUser = getCurrentUser();
        log.info("User {} initiating RTGS transfer", currentUser.getUsername());

        // Validate RTGS operating hours
        validateRTGSOperatingHours();

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

        // Validate minimum RTGS amount
        if (request.getAmount().compareTo(RTGS_MINIMUM_AMOUNT) < 0) {
            throw new IllegalArgumentException("RTGS requires minimum amount of ₹2,00,000. Current amount: ₹" + 
                    request.getAmount());
        }

        // Validate and get beneficiary
        Beneficiary beneficiary = beneficiaryService.validateBeneficiaryForTransfer(
                request.getBeneficiaryId(), 
                currentUser.getCustomer().getId()
        );

        // Calculate charges
        BigDecimal charges = calculateRTGSCharges(request.getAmount());
        BigDecimal totalAmount = request.getAmount().add(charges);

        // Validate sufficient balance
        if (sourceAccount.getAvailableBalance().compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: ₹" + 
                    sourceAccount.getAvailableBalance() + ", Required: ₹" + totalAmount);
        }

        // Validate minimum balance requirement
        BigDecimal balanceAfterTransfer = sourceAccount.getBalance().subtract(totalAmount);
        if (balanceAfterTransfer.compareTo(sourceAccount.getMinimumBalance()) < 0) {
            throw new IllegalArgumentException("Transfer would violate minimum balance requirement of ₹" + 
                    sourceAccount.getMinimumBalance());
        }

        // Debit from source account
        BigDecimal balanceBefore = sourceAccount.getBalance();
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(totalAmount));
        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(totalAmount));
        sourceAccount.setLastTransactionDate(LocalDateTime.now());
        accountRepository.save(sourceAccount);

        // Create EFT transaction record (initial status: PROCESSING)
        EFTTransaction eftTransaction = EFTTransaction.builder()
                .eftType(EFTTransaction.TYPE_RTGS)
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
                .status(EFTTransaction.STATUS_PROCESSING)
                .initiatedBy(currentUser.getUsername())
                .build();

        EFTTransaction savedEFT = eftTransactionRepository.save(eftTransaction);

        // Create internal transaction record (for debit) - status PROCESSING initially
        Transaction transaction = Transaction.builder()
                .account(sourceAccount)
                .transactionType(Transaction.TYPE_DEBIT)
                .amount(totalAmount)
                .currency(sourceAccount.getCurrency())
                .balanceBefore(balanceBefore)
                .balanceAfter(sourceAccount.getBalance())
                .description("RTGS Transfer to " + beneficiary.getBeneficiaryName() + " - " + request.getPurpose())
                .category(Transaction.CATEGORY_TRANSFER)
                .status(Transaction.STATUS_PROCESSING)
                .transactionDate(LocalDateTime.now())
                .valueDate(LocalDateTime.now())
                .initiatedBy(currentUser.getUsername())
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Link transaction to EFT
        savedEFT.setTransaction(savedTransaction);
        eftTransactionRepository.save(savedEFT);

        // Process RTGS in real-time
        try {
            log.info("Processing RTGS transfer in real-time. Reference: {}, Amount: ₹{}", 
                    savedEFT.getEftReference(), request.getAmount());
            
            processRTGSTransaction(savedEFT);

            // Update EFT status to COMPLETED
            savedEFT.setStatus(EFTTransaction.STATUS_COMPLETED);
            savedEFT.setActualCompletion(LocalDateTime.now());
            savedEFT.setProcessedBy("RTGS_PROCESSOR");
            eftTransactionRepository.save(savedEFT);

            // Update internal transaction status to COMPLETED
            savedTransaction.setStatus(Transaction.STATUS_COMPLETED);
            savedTransaction.setApprovedBy("RTGS_PROCESSOR");
            savedTransaction.setApprovalDate(LocalDateTime.now());
            transactionRepository.save(savedTransaction);

            log.info("RTGS transfer completed successfully. Reference: {}", savedEFT.getEftReference());

            return RTGSTransferResponse.success(
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
                    savedEFT.getActualCompletion(),
                    savedEFT.getCreatedAt()
            );

        } catch (Exception e) {
            // Mark EFT as failed
            savedEFT.setStatus(EFTTransaction.STATUS_FAILED);
            savedEFT.setFailureReason(e.getMessage());
            savedEFT.setActualCompletion(LocalDateTime.now());
            eftTransactionRepository.save(savedEFT);

            // Mark internal transaction as FAILED
            savedTransaction.setStatus(Transaction.STATUS_FAILED);
            savedTransaction.setFailureReason(e.getMessage());
            transactionRepository.save(savedTransaction);

            // Refund amount to source account
            refundFailedTransfer(savedEFT);

            log.error("RTGS transfer failed: {}. Error: {}", savedEFT.getEftReference(), e.getMessage());

            throw new RuntimeException("RTGS transfer failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get RTGS transaction status
     */
    @Transactional(readOnly = true)
    public EFTStatusResponse getRTGSStatus(String eftReference) {
        User currentUser = getCurrentUser();

        EFTTransaction eft = eftTransactionRepository.findByEftReference(eftReference)
                .orElseThrow(() -> new IllegalArgumentException("RTGS transaction not found: " + eftReference));

        // Verify it's an RTGS transaction
        if (!eft.isRTGS()) {
            throw new IllegalArgumentException("Transaction is not an RTGS transfer");
        }

        // Security check
        if (!hasAccessToAccount(currentUser, eft.getSourceAccount())) {
            throw new SecurityException("Access denied");
        }

        return mapToStatusResponse(eft);
    }

    /**
     * Get RTGS history for user's accounts
     */
    @Transactional(readOnly = true)
    public List<EFTStatusResponse> getRTGSHistory(String accountNumber) {
        User currentUser = getCurrentUser();

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!hasAccessToAccount(currentUser, account)) {
            throw new SecurityException("Access denied");
        }

        List<EFTTransaction> transactions = eftTransactionRepository
                .findBySourceAccountIdOrderByCreatedAtDesc(account.getId());

        return transactions.stream()
                .filter(EFTTransaction::isRTGS)
                .map(this::mapToStatusResponse)
                .collect(Collectors.toList());
    }

    /**
     * Process individual RTGS transaction (simulated real-time processing)
     */
    private void processRTGSTransaction(EFTTransaction eft) {
        log.info("Simulating real-time RTGS transfer to {} for amount ₹{}", 
                eft.getBeneficiaryBankName(), eft.getAmount());

        // Simulate external RTGS network processing
        // In production, this would integrate with RBI's RTGS system
        try {
            // Simulate processing delay (1-2 seconds for real-time processing)
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Random failure simulation (2% failure rate for demo)
        if (Math.random() < 0.02) {
            throw new RuntimeException("Beneficiary bank system unavailable or account not found");
        }

        // Success - external bank has credited the beneficiary in real-time
        log.info("RTGS transfer successful: {}", eft.getEftReference());
    }

    /**
     * Refund amount if RTGS fails
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
                    .description("Refund - RTGS transfer failed: " + eft.getEftReference())
                    .category(Transaction.CATEGORY_REFUND)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(LocalDateTime.now())
                    .initiatedBy("SYSTEM")
                    .approvedBy("SYSTEM")
                    .approvalDate(LocalDateTime.now())
                    .build();
            transactionRepository.save(refundTransaction);

            log.info("Refund processed for failed RTGS: {}", eft.getEftReference());
        } catch (Exception e) {
            log.error("Error processing refund for failed RTGS: {}", eft.getEftReference(), e);
        }
    }

    // Helper methods

    /**
     * Validate RTGS operating hours
     */
    private void validateRTGSOperatingHours() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        // Check if it's a weekend
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException(
                    "RTGS is not available on weekends. Operating hours: Monday to Friday, 9:00 AM to 4:30 PM");
        }

        // Check if within operating hours
        if (currentTime.isBefore(RTGS_START_TIME) || currentTime.isAfter(RTGS_END_TIME)) {
            throw new IllegalArgumentException(
                    String.format("RTGS is not available at this time. Operating hours: 9:00 AM to 4:30 PM (Current time: %s)",
                            currentTime.toString()));
        }
    }

    /**
     * Calculate RTGS charges based on amount
     */
    private BigDecimal calculateRTGSCharges(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("500000")) <= 0) {
            // 2L to 5L
            return RTGS_CHARGE_2L_TO_5L;
        } else {
            // Above 5L
            return RTGS_CHARGE_ABOVE_5L;
        }
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

