package com.nedbank.banking.service;

import com.nedbank.banking.dto.*;
import com.nedbank.banking.entity.*;
import com.nedbank.banking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for UPI operations
 * Handles UPI ID registration and payment processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UPIService {

    private final UPIAccountRepository upiAccountRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final QRTransactionRepository qrTransactionRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Register a UPI ID for an account
     */
    @Transactional
    public UPIRegisterResponse registerUPI(UPIRegisterRequest request) {
        User currentUser = getCurrentUser();
        log.info("User {} registering UPI ID: {}", currentUser.getUsername(), request.getUpiId());

        try {
            // Check if UPI ID already exists
            if (upiAccountRepository.existsByUpiId(request.getUpiId())) {
                return UPIRegisterResponse.failure("UPI ID is already registered");
            }

            // Validate and get account
            Account account = accountRepository.findByAccountNumberWithCustomer(request.getAccountNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.getAccountNumber()));

            // Security: Ensure user owns the account
            if (!hasAccessToAccount(currentUser, account)) {
                throw new SecurityException("You are not authorized to link UPI to this account");
            }

            // Validate account is active
            if (!account.isActive()) {
                return UPIRegisterResponse.failure("Account is not active");
            }

            // Extract UPI provider from UPI ID
            String upiProvider = UPIAccount.extractProvider(request.getUpiId());

            // If setting as primary, unset other primary UPIs for this user
            if (request.isPrimary()) {
                List<UPIAccount> existingUPIs = upiAccountRepository.findByUserId(currentUser.getId());
                existingUPIs.forEach(upi -> {
                    upi.setIsPrimary(false);
                    upiAccountRepository.save(upi);
                });
            }

            // Create UPI account
            UPIAccount upiAccount = UPIAccount.builder()
                    .upiId(request.getUpiId())
                    .account(account)
                    .user(currentUser)
                    .upiProvider(upiProvider)
                    .isPrimary(request.isPrimary())
                    .status(UPIAccount.STATUS_ACTIVE)
                    .isVerified(true)  // Auto-verify for now
                    .verifiedAt(LocalDateTime.now())
                    .build();

            UPIAccount savedUPI = upiAccountRepository.save(upiAccount);

            log.info("UPI ID {} registered successfully for account {}", 
                    request.getUpiId(), request.getAccountNumber());

            return UPIRegisterResponse.success(
                    savedUPI.getId(),
                    savedUPI.getUpiId(),
                    account.getAccountNumber(),
                    upiProvider,
                    request.isPrimary(),
                    savedUPI.getStatus(),
                    savedUPI.getCreatedAt()
            );

        } catch (Exception e) {
            log.error("Failed to register UPI: {}", e.getMessage(), e);
            return UPIRegisterResponse.failure("Failed to register UPI: " + e.getMessage());
        }
    }

    /**
     * Get all UPI IDs for current user
     */
    @Transactional(readOnly = true)
    public List<UPIAccountResponse> getMyUPIAccounts() {
        User currentUser = getCurrentUser();
        
        List<UPIAccount> upiAccounts = upiAccountRepository.findByUserId(currentUser.getId());
        
        return upiAccounts.stream()
                .map(this::mapToUPIAccountResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get UPI account by UPI ID
     */
    @Transactional(readOnly = true)
    public UPIAccountResponse getUPIAccountByUpiId(String upiId) {
        UPIAccount upiAccount = upiAccountRepository.findByUpiId(upiId)
                .orElseThrow(() -> new IllegalArgumentException("UPI ID not found: " + upiId));

        User currentUser = getCurrentUser();
        
        // Check access
        if (!hasAccessToAccount(currentUser, upiAccount.getAccount())) {
            throw new SecurityException("You don't have permission to view this UPI account");
        }

        return mapToUPIAccountResponse(upiAccount);
    }

    /**
     * Process UPI payment (direct internal transfer)
     */
    @Transactional
    public UPIPaymentResponse initiateUPIPayment(UPIPaymentRequest request) {
        User currentUser = getCurrentUser();
        log.info("User {} processing UPI payment to: {}", 
                currentUser.getUsername(), request.getReceiverUpiId());

        try {
            // Find receiver's UPI account
            UPIAccount receiverUPI = upiAccountRepository.findActiveByUpiId(request.getReceiverUpiId())
                    .orElseThrow(() -> new IllegalArgumentException("Receiver UPI ID not found or inactive"));

            Account receiverAccount = receiverUPI.getAccount();

            // Validate receiver account is active
            if (!receiverAccount.isActive()) {
                return UPIPaymentResponse.failure("Receiver account is not active");
            }

            // Get payer account
            Account payerAccount = accountRepository.findByAccountNumberWithCustomer(request.getPayerAccountNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Payer account not found"));

            // Security: Ensure user owns the payer account
            if (!hasAccessToAccount(currentUser, payerAccount)) {
                throw new SecurityException("You are not authorized to pay from this account");
            }

            // Validate payer account is active
            if (!payerAccount.isActive()) {
                return UPIPaymentResponse.failure("Payer account is not active");
            }

            // Cannot pay to same account
            if (payerAccount.getId().equals(receiverAccount.getId())) {
                return UPIPaymentResponse.failure("Cannot pay to the same account");
            }

            // Validate sufficient balance
            if (payerAccount.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                return UPIPaymentResponse.failure("Insufficient balance. Available: " + 
                        payerAccount.getAvailableBalance() + ", Required: " + request.getAmount());
            }

            // Process internal transfer
            LocalDateTime now = LocalDateTime.now();
            BigDecimal amount = request.getAmount();
            BigDecimal payerBalanceBefore = payerAccount.getBalance();

            // Debit from payer
            payerAccount.debit(amount);
            accountRepository.save(payerAccount);

            Transaction debitTransaction = Transaction.builder()
                    .account(payerAccount)
                    .destinationAccount(receiverAccount)
                    .transactionType(Transaction.TYPE_DEBIT)
                    .amount(amount)
                    .currency(request.getCurrency())
                    .balanceBefore(payerBalanceBefore)
                    .balanceAfter(payerAccount.getBalance())
                    .description("UPI Payment to " + request.getReceiverUpiId())
                    .category(Transaction.CATEGORY_PAYMENT)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(now)
                    .valueDate(now)
                    .initiatedBy(currentUser.getUsername())
                    .approvedBy(currentUser.getUsername())
                    .approvalDate(now)
                    .build();

            Transaction savedDebitTxn = transactionRepository.save(debitTransaction);

            // Credit to receiver
            BigDecimal receiverBalanceBefore = receiverAccount.getBalance();
            receiverAccount.credit(amount);
            accountRepository.save(receiverAccount);

            Transaction creditTransaction = Transaction.builder()
                    .account(receiverAccount)
                    .destinationAccount(payerAccount)
                    .transactionType(Transaction.TYPE_CREDIT)
                    .amount(amount)
                    .currency(request.getCurrency())
                    .balanceBefore(receiverBalanceBefore)
                    .balanceAfter(receiverAccount.getBalance())
                    .description("UPI Payment from " + currentUser.getUsername())
                    .category(Transaction.CATEGORY_PAYMENT)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(now)
                    .valueDate(now)
                    .initiatedBy(currentUser.getUsername())
                    .approvedBy("system")
                    .approvalDate(now)
                    .build();

            Transaction savedCreditTxn = transactionRepository.save(creditTransaction);

            // Create UPI transaction record
            QRTransaction upiTransaction = QRTransaction.builder()
                    .paymentType(QRTransaction.TYPE_UPI)
                    .upiAccount(receiverUPI)
                    .payerAccount(payerAccount)
                    .receiverAccount(receiverAccount)
                    .amount(amount)
                    .netAmount(amount)
                    .currency(request.getCurrency())
                    .status(QRTransaction.STATUS_SETTLED)
                    .paymentMethod("INTERNAL")
                    .description(request.getDescription())
                    .initiatedBy(currentUser.getUsername())
                    .debitTransaction(savedDebitTxn)
                    .creditTransaction(savedCreditTxn)
                    .settledAt(now)
                    .build();

            QRTransaction savedTransaction = qrTransactionRepository.save(upiTransaction);

            // Update UPI last used
            receiverUPI.markAsUsed();
            upiAccountRepository.save(receiverUPI);

            log.info("UPI payment completed successfully. Transaction: {}", 
                    savedTransaction.getTransactionReference());

            String receiverName = receiverAccount.getCustomer().getFirstName() + " " +
                                 receiverAccount.getCustomer().getLastName();

            return UPIPaymentResponse.success(
                    savedTransaction.getTransactionReference(),
                    request.getReceiverUpiId(),
                    receiverAccount.getAccountNumber(),
                    receiverName,
                    payerAccount.getAccountNumber(),
                    amount,
                    request.getCurrency(),
                    payerBalanceBefore,
                    payerAccount.getBalance(),
                    now,
                    savedDebitTxn.getId(),
                    savedCreditTxn.getId()
            );

        } catch (Exception e) {
            log.error("Error processing UPI payment: {}", e.getMessage(), e);
            return UPIPaymentResponse.failure("Failed to process payment: " + e.getMessage());
        }
    }

    /**
     * Delete/Deactivate UPI ID
     */
    @Transactional
    public void deactivateUPI(String upiId) {
        User currentUser = getCurrentUser();
        
        UPIAccount upiAccount = upiAccountRepository.findByUpiId(upiId)
                .orElseThrow(() -> new IllegalArgumentException("UPI ID not found"));

        // Check access
        if (!hasAccessToAccount(currentUser, upiAccount.getAccount())) {
            throw new SecurityException("You don't have permission to deactivate this UPI ID");
        }

        upiAccount.setStatus(UPIAccount.STATUS_INACTIVE);
        upiAccountRepository.save(upiAccount);

        log.info("UPI ID {} deactivated by user {}", upiId, currentUser.getUsername());
    }

    /**
     * Set UPI as primary
     */
    @Transactional
    public void setPrimaryUPI(String upiId) {
        User currentUser = getCurrentUser();
        
        UPIAccount upiAccount = upiAccountRepository.findByUpiId(upiId)
                .orElseThrow(() -> new IllegalArgumentException("UPI ID not found"));

        // Check access
        if (!hasAccessToAccount(currentUser, upiAccount.getAccount())) {
            throw new SecurityException("You don't have permission to modify this UPI ID");
        }

        // Unset other primary UPIs
        List<UPIAccount> existingUPIs = upiAccountRepository.findByUserId(currentUser.getId());
        existingUPIs.forEach(upi -> {
            upi.setIsPrimary(false);
            upiAccountRepository.save(upi);
        });

        // Set as primary
        upiAccount.setIsPrimary(true);
        upiAccountRepository.save(upiAccount);

        log.info("UPI ID {} set as primary by user {}", upiId, currentUser.getUsername());
    }

    /**
     * Get UPI transaction history
     */
    @Transactional(readOnly = true)
    public List<QRTransactionResponse> getUPITransactionHistory(String accountNumber) {
        User currentUser = getCurrentUser();
        
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!hasAccessToAccount(currentUser, account)) {
            throw new SecurityException("You don't have permission to view transactions for this account");
        }

        List<QRTransaction> transactions = qrTransactionRepository.findByAccountId(account.getId())
                .stream()
                .filter(txn -> QRTransaction.TYPE_UPI.equals(txn.getPaymentType()))
                .collect(Collectors.toList());
        
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    // Helper methods
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

    private UPIAccountResponse mapToUPIAccountResponse(UPIAccount upiAccount) {
        return UPIAccountResponse.builder()
                .id(upiAccount.getId())
                .upiId(upiAccount.getUpiId())
                .accountNumber(upiAccount.getAccount().getAccountNumber())
                .upiProvider(upiAccount.getUpiProvider())
                .isPrimary(upiAccount.getIsPrimary())
                .status(upiAccount.getStatus())
                .isVerified(upiAccount.getIsVerified())
                .verifiedAt(upiAccount.getVerifiedAt())
                .lastUsedAt(upiAccount.getLastUsedAt())
                .createdAt(upiAccount.getCreatedAt())
                .build();
    }

    private QRTransactionResponse mapToTransactionResponse(QRTransaction qrTxn) {
        return QRTransactionResponse.builder()
                .id(qrTxn.getId())
                .transactionReference(qrTxn.getTransactionReference())
                .paymentType(qrTxn.getPaymentType())
                .razorpayPaymentId(qrTxn.getRazorpayPaymentId())
                .razorpayOrderId(qrTxn.getRazorpayOrderId())
                .payerAccountNumber(qrTxn.getPayerAccount().getAccountNumber())
                .receiverAccountNumber(qrTxn.getReceiverAccount().getAccountNumber())
                .receiverName(qrTxn.getReceiverAccount().getCustomer().getFirstName() + " " +
                             qrTxn.getReceiverAccount().getCustomer().getLastName())
                .amount(qrTxn.getAmount())
                .razorpayFee(qrTxn.getRazorpayFee())
                .netAmount(qrTxn.getNetAmount())
                .currency(qrTxn.getCurrency())
                .status(qrTxn.getStatus())
                .paymentMethod(qrTxn.getPaymentMethod())
                .description(qrTxn.getDescription())
                .initiatedBy(qrTxn.getInitiatedBy())
                .failureReason(qrTxn.getFailureReason())
                .initiatedAt(qrTxn.getInitiatedAt())
                .settledAt(qrTxn.getSettledAt())
                .debitTransactionId(qrTxn.getDebitTransaction() != null ? qrTxn.getDebitTransaction().getId() : null)
                .creditTransactionId(qrTxn.getCreditTransaction() != null ? qrTxn.getCreditTransaction().getId() : null)
                .build();
    }
}

