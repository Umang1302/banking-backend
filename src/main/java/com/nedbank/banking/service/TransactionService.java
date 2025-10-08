package com.nedbank.banking.service;

import com.nedbank.banking.dto.*;
import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.Transaction;
import com.nedbank.banking.entity.User;
import com.nedbank.banking.repository.AccountRepository;
import com.nedbank.banking.repository.TransactionRepository;
import com.nedbank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    
    /**
     * Validate account and get holder details (for transfer validation)
     */
    public Map<String, Object> validateAccountForTransfer(String accountNumber) {
        Account account = accountRepository.findByAccountNumberWithCustomer(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
        
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active and cannot receive transfers");
        }
        
        if (account.getCustomer() == null) {
            throw new IllegalArgumentException("Account has no associated customer");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("accountNumber", account.getAccountNumber());
        result.put("accountType", account.getAccountType());
        result.put("accountHolderName", account.getCustomer().getFirstName() + " " + 
                                       account.getCustomer().getLastName());
        result.put("currency", account.getCurrency());
        
        return result;
    }
    
    /**
     * Transfer money between user accounts
     */
    @Transactional
    public MoneyTransferResponse transferMoney(MoneyTransferRequest request) {
        User currentUser = getCurrentUser();
        log.info("User {} initiating money transfer from {} to {}", 
                currentUser.getUsername(), request.getFromAccountNumber(), request.getToAccountNumber());
        
        // Validate accounts exist (with customer eagerly loaded)
        Account sourceAccount = accountRepository.findByAccountNumberWithCustomer(request.getFromAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + request.getFromAccountNumber()));
        
        Account destinationAccount = accountRepository.findByAccountNumberWithCustomer(request.getToAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + request.getToAccountNumber()));
        
        // Security: Ensure user can only send from their own accounts
        if (!hasAccessToAccount(currentUser, sourceAccount)) {
            log.warn("User {} attempted to transfer from unauthorized account {}", 
                    currentUser.getUsername(), request.getFromAccountNumber());
            throw new SecurityException("You are not authorized to transfer from this account");
        }
        
        // Validate: Cannot transfer to same account
        if (sourceAccount.getId().equals(destinationAccount.getId())) {
            throw new IllegalArgumentException("Cannot transfer money to the same account");
        }
        
        // Validate: Source account must be ACTIVE
        if (!"ACTIVE".equals(sourceAccount.getStatus())) {
            throw new IllegalArgumentException("Source account is not active");
        }
        
        // Validate: Destination account must be ACTIVE
        if (!"ACTIVE".equals(destinationAccount.getStatus())) {
            throw new IllegalArgumentException("Destination account is not active");
        }
        
        // Validate: Sufficient balance
        if (sourceAccount.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + 
                    sourceAccount.getAvailableBalance() + ", Required: " + request.getAmount());
        }
        
        // Validate: Minimum balance requirement
        BigDecimal balanceAfterTransfer = sourceAccount.getBalance().subtract(request.getAmount());
        if (balanceAfterTransfer.compareTo(sourceAccount.getMinimumBalance()) < 0) {
            throw new IllegalArgumentException("Transfer would violate minimum balance requirement of " + 
                    sourceAccount.getMinimumBalance());
        }

        String transferReference = generateTransactionReference();
        String debitTxnReference = generateTransactionReference();
        String creditTxnReference = generateTransactionReference();
        LocalDateTime transactionDate = LocalDateTime.now();
        
        // Record balances before transfer
        BigDecimal senderBalanceBefore = sourceAccount.getBalance();
        
        // DEBIT from source account
        BigDecimal senderBalanceAfter = sourceAccount.getBalance().subtract(request.getAmount());
        sourceAccount.setBalance(senderBalanceAfter);
        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(request.getAmount()));
        sourceAccount.setLastTransactionDate(transactionDate);
        accountRepository.save(sourceAccount);
        
        Transaction debitTransaction = Transaction.builder()
                .transactionReference(debitTxnReference)  // Unique reference
                .externalReference(transferReference)      // Common linking reference
                .account(sourceAccount)
                .destinationAccount(destinationAccount)
                .transactionType(Transaction.TYPE_DEBIT)
                .amount(request.getAmount())
                .currency(sourceAccount.getCurrency())
                .balanceBefore(senderBalanceBefore)
                .balanceAfter(senderBalanceAfter)
                .description(request.getDescription())
                .category(Transaction.CATEGORY_TRANSFER)
                .status(Transaction.STATUS_COMPLETED)
                .transactionDate(transactionDate)
                .valueDate(transactionDate)
                .initiatedBy(currentUser.getUsername())
                .approvedBy(currentUser.getUsername())
                .approvalDate(transactionDate)
                .isBulkUpload(false)
                .build();
        
        Transaction savedDebitTxn = transactionRepository.save(debitTransaction);
        
        // CREDIT to destination account
        BigDecimal recipientBalanceBefore = destinationAccount.getBalance();
        BigDecimal recipientBalanceAfter = recipientBalanceBefore.add(request.getAmount());
        destinationAccount.setBalance(recipientBalanceAfter);
        destinationAccount.setAvailableBalance(destinationAccount.getAvailableBalance().add(request.getAmount()));
        destinationAccount.setLastTransactionDate(transactionDate);
        accountRepository.save(destinationAccount);
        
        Transaction creditTransaction = Transaction.builder()
                .transactionReference(creditTxnReference)  // Unique reference
                .externalReference(transferReference)       // Common linking reference
                .account(destinationAccount)
                .destinationAccount(sourceAccount)
                .transactionType(Transaction.TYPE_CREDIT)
                .amount(request.getAmount())
                .currency(destinationAccount.getCurrency())
                .balanceBefore(recipientBalanceBefore)
                .balanceAfter(recipientBalanceAfter)
                .description("Transfer from " + sourceAccount.getAccountNumber() + " - " + request.getDescription())
                .category(Transaction.CATEGORY_TRANSFER)
                .status(Transaction.STATUS_COMPLETED)
                .transactionDate(transactionDate)
                .valueDate(transactionDate)
                .initiatedBy(currentUser.getUsername())
                .approvedBy("system")
                .approvalDate(transactionDate)
                .isBulkUpload(false)
                .build();
        
        Transaction savedCreditTxn = transactionRepository.save(creditTransaction);
        
        // Get recipient name
        String recipientName = destinationAccount.getCustomer().getFirstName() + " " + 
                              destinationAccount.getCustomer().getLastName();
        
        log.info("Money transfer successful. Transfer Reference: {}, Amount: {}, From: {} to {}", 
                transferReference, request.getAmount(), 
                request.getFromAccountNumber(), request.getToAccountNumber());
        
        return MoneyTransferResponse.success(
                transferReference,  // Use the common transfer reference for tracking
                sourceAccount.getAccountNumber(),
                destinationAccount.getAccountNumber(),
                recipientName,
                request.getAmount(),
                sourceAccount.getCurrency(),
                senderBalanceBefore,
                senderBalanceAfter,
                request.getDescription(),
                transactionDate,
                savedDebitTxn.getId(),
                savedCreditTxn.getId()
        );
    }

    /**
     * Create a new transaction (debit/credit/transfer)
     */
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User currentUser = getCurrentUser();
        log.info("User {} initiating transaction", currentUser.getUsername());

        // Validate and get source account
        Account sourceAccount = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + request.getAccountNumber()));

        // Check if user has access to this account
        if (!hasAccessToAccount(currentUser, sourceAccount)) {
            throw new SecurityException("You don't have permission to access this account");
        }

        // Validate account is active
        if (!"ACTIVE".equals(sourceAccount.getStatus())) {
            throw new IllegalStateException("Account is not active: " + sourceAccount.getAccountNumber());
        }

        // Handle different transaction types
        Transaction transaction;
        switch (request.getTransactionType()) {
            case "DEBIT":
            case "WITHDRAWAL":
                transaction = processDebit(sourceAccount, request, currentUser);
                break;
            case "CREDIT":
            case "DEPOSIT":
                transaction = processCredit(sourceAccount, request, currentUser);
                break;
            case "TRANSFER":
                transaction = processTransfer(sourceAccount, request, currentUser);
                break;
            default:
                throw new IllegalArgumentException("Invalid transaction type: " + request.getTransactionType());
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        accountRepository.save(sourceAccount);

        log.info("Transaction {} created successfully. Reference: {}", 
                savedTransaction.getId(), savedTransaction.getTransactionReference());

        return mapToTransactionResponse(savedTransaction);
    }

    /**
     * Process debit/withdrawal transaction
     */
    private Transaction processDebit(Account account, TransactionRequest request, User user) {
        BigDecimal amount = request.getAmount();
        
        // Check sufficient balance
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance. Available: " + account.getAvailableBalance());
        }

        // Check minimum balance requirement
        BigDecimal newBalance = account.getBalance().subtract(amount);
        if (newBalance.compareTo(account.getMinimumBalance()) < 0) {
            throw new IllegalStateException("Transaction would breach minimum balance requirement");
        }

        BigDecimal balanceBefore = account.getBalance();
        
        // Update account balance
        account.setBalance(newBalance);
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setLastTransactionDate(LocalDateTime.now());

        return Transaction.builder()
                .account(account)
                .transactionType(request.getTransactionType())
                .amount(amount)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .description(request.getDescription())
                .category(request.getCategory())
                .referenceNumber(request.getReferenceNumber())
                .status(Transaction.STATUS_COMPLETED)
                .transactionDate(LocalDateTime.now())
                .valueDate(LocalDateTime.now())
                .initiatedBy(user.getUsername())
                .approvedBy(user.getUsername())
                .approvalDate(LocalDateTime.now())
                .build();
    }

    /**
     * Process credit/deposit transaction
     */
    private Transaction processCredit(Account account, TransactionRequest request, User user) {
        BigDecimal amount = request.getAmount();
        BigDecimal balanceBefore = account.getBalance();
        BigDecimal newBalance = balanceBefore.add(amount);

        // Update account balance
        account.setBalance(newBalance);
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setLastTransactionDate(LocalDateTime.now());

        return Transaction.builder()
                .account(account)
                .transactionType(request.getTransactionType())
                .amount(amount)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .description(request.getDescription())
                .category(request.getCategory())
                .referenceNumber(request.getReferenceNumber())
                .status(Transaction.STATUS_COMPLETED)
                .transactionDate(LocalDateTime.now())
                .valueDate(LocalDateTime.now())
                .initiatedBy(user.getUsername())
                .approvedBy(user.getUsername())
                .approvalDate(LocalDateTime.now())
                .build();
    }

    /**
     * Process transfer transaction
     */
    private Transaction processTransfer(Account sourceAccount, TransactionRequest request, User user) {
        if (request.getDestinationAccountNumber() == null) {
            throw new IllegalArgumentException("Destination account is required for transfers");
        }

        Account destinationAccount = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        if (!destinationAccount.getStatus().equals("ACTIVE")) {
            throw new IllegalStateException("Destination account is not active");
        }

        BigDecimal amount = request.getAmount();
        
        // Check sufficient balance
        if (sourceAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        BigDecimal sourceBalanceBefore = sourceAccount.getBalance();

        // Update source account
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(amount));
        sourceAccount.setLastTransactionDate(LocalDateTime.now());

        // Update destination account
        destinationAccount.setBalance(destinationAccount.getBalance().add(amount));
        destinationAccount.setAvailableBalance(destinationAccount.getAvailableBalance().add(amount));
        destinationAccount.setLastTransactionDate(LocalDateTime.now());

        accountRepository.save(destinationAccount);

        return Transaction.builder()
                .account(sourceAccount)
                .destinationAccount(destinationAccount)
                .transactionType(Transaction.TYPE_TRANSFER)
                .amount(amount)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .balanceBefore(sourceBalanceBefore)
                .balanceAfter(sourceAccount.getBalance())
                .description(request.getDescription())
                .category(Transaction.CATEGORY_TRANSFER)
                .referenceNumber(request.getReferenceNumber())
                .status(Transaction.STATUS_COMPLETED)
                .transactionDate(LocalDateTime.now())
                .valueDate(LocalDateTime.now())
                .initiatedBy(user.getUsername())
                .approvedBy(user.getUsername())
                .approvalDate(LocalDateTime.now())
                .build();
    }

    /**
     * Get transaction history for an account
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(String accountNumber, LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentUser();
        
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        // Check access
        if (!hasAccessToAccount(currentUser, account)) {
            throw new SecurityException("You don't have permission to view transactions for this account");
        }

        List<Transaction> transactions;
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            transactions = transactionRepository.findByAccountIdAndDateRange(account.getId(), start, end);
        } else {
            transactions = transactionRepository.findByAccountIdOrderByTransactionDateDesc(account.getId());
        }

        log.info("Retrieved {} transactions for account {}", transactions.size(), accountNumber);
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get transaction by reference
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String reference) {
        Transaction transaction = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + reference));

        User currentUser = getCurrentUser();
        if (!hasAccessToAccount(currentUser, transaction.getAccount())) {
            throw new SecurityException("You don't have permission to view this transaction");
        }

        return mapToTransactionResponse(transaction);
    }

    /**
     * Bulk upload transactions from CSV file (Accountants only)
     */
    @Transactional
    public BulkTransactionResponse bulkUploadTransactions(MultipartFile file) {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("TRANSACTION_WRITE")) {
            throw new SecurityException("You don't have permission to upload bulk transactions");
        }

        log.info("Processing bulk transaction upload by {}", currentUser.getUsername());

        String batchId = "BATCH" + UUID.randomUUID().toString().substring(0, 8);
        List<TransactionResponse> successfulTransactions = new ArrayList<>();
        List<BulkTransactionResponse.BulkTransactionError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int rowNumber = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                
                // Skip header row
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                try {
                    String[] fields = line.split(",");
                    if (fields.length < 3) {
                        errors.add(BulkTransactionResponse.BulkTransactionError.builder()
                                .rowNumber(rowNumber)
                                .accountNumber("N/A")
                                .error("Invalid CSV format")
                                .build());
                        continue;
                    }

                    BulkTransactionRequest bulkRequest = BulkTransactionRequest.builder()
                            .accountNumber(fields[0].trim())
                            .transactionType(fields[1].trim())
                            .amount(new BigDecimal(fields[2].trim()))
                            .description(fields.length > 3 ? fields[3].trim() : "Bulk upload")
                            .category(fields.length > 4 ? fields[4].trim() : "OTHER")
                            .build();

                    TransactionRequest request = TransactionRequest.builder()
                            .accountNumber(bulkRequest.getAccountNumber())
                            .transactionType(bulkRequest.getTransactionType())
                            .amount(bulkRequest.getAmount())
                            .description(bulkRequest.getDescription())
                            .category(bulkRequest.getCategory())
                            .build();

                    TransactionResponse response = createBulkTransaction(request, batchId, currentUser);
                    successfulTransactions.add(response);

                } catch (Exception e) {
                    errors.add(BulkTransactionResponse.BulkTransactionError.builder()
                            .rowNumber(rowNumber)
                            .accountNumber("N/A")
                            .error(e.getMessage())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Error processing bulk upload: {}", e.getMessage());
            throw new RuntimeException("Failed to process bulk upload file", e);
        }

        log.info("Bulk upload complete. Batch: {}, Success: {}, Failed: {}", 
                batchId, successfulTransactions.size(), errors.size());

        return BulkTransactionResponse.builder()
                .batchId(batchId)
                .totalTransactions(successfulTransactions.size() + errors.size())
                .successfulTransactions(successfulTransactions.size())
                .failedTransactions(errors.size())
                .successfulResults(successfulTransactions)
                .errors(errors)
                .message("Bulk upload processed: " + successfulTransactions.size() + " successful, " + errors.size() + " failed")
                .build();
    }

    /**
     * Create transaction from bulk upload
     */
    private TransactionResponse createBulkTransaction(TransactionRequest request, String batchId, User user) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.getAccountNumber()));

        Transaction transaction;
        if (request.getTransactionType().equals("DEBIT") || request.getTransactionType().equals("WITHDRAWAL")) {
            transaction = processDebit(account, request, user);
        } else {
            transaction = processCredit(account, request, user);
        }

        transaction.setIsBulkUpload(true);
        transaction.setBulkUploadBatchId(batchId);

        Transaction saved = transactionRepository.save(transaction);
        accountRepository.save(account);

        return mapToTransactionResponse(saved);
    }

    /**
     * Get all transactions (admin/accountant only)
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions() {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("TRANSACTION_READ")) {
            throw new SecurityException("You don't have permission to view all transactions");
        }

        List<Transaction> transactions = transactionRepository.findAll();
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get pending transactions (admin/accountant only)
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getPendingTransactions() {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("TRANSACTION_READ")) {
            throw new SecurityException("You don't have permission to view pending transactions");
        }

        List<Transaction> transactions = transactionRepository.findByStatus(Transaction.STATUS_PENDING);
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
        // Admins and accountants can access all accounts
        if (user.hasPermission("ACCOUNT_READ")) {
            return true;
        }
        
        // Customers can only access their own accounts
        if (user.getCustomer() != null) {
            return account.getCustomer().getId().equals(user.getCustomer().getId());
        }
        
        return false;
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .accountNumber(transaction.getAccount().getAccountNumber())
                .destinationAccountNumber(transaction.getDestinationAccount() != null ? 
                        transaction.getDestinationAccount().getAccountNumber() : null)
                .description(transaction.getDescription())
                .category(transaction.getCategory())
                .status(transaction.getStatus())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .transactionDate(transaction.getTransactionDate())
                .initiatedBy(transaction.getInitiatedBy())
                .referenceNumber(transaction.getReferenceNumber())
                .failureReason(transaction.getFailureReason())
                .build();
    }
    
    /**
     * Generate unique transaction reference
     */
    private String generateTransactionReference() {
        // Use nanoTime for better uniqueness when generating multiple references quickly
        return "TXN" + System.nanoTime() + UUID.randomUUID().toString().substring(0, 8).toUpperCase().replace("-", "");
    }
}

