package com.nedbank.banking.service;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.nedbank.banking.dto.*;
import com.nedbank.banking.entity.*;
import com.nedbank.banking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for QR Payment orchestration
 * Handles QR code generation, payment processing, and settlement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QRPaymentService {

    private final QRPaymentRequestRepository qrPaymentRequestRepository;
    private final QRTransactionRepository qrTransactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final QRCodeService qrCodeService;

    /**
     * Generate QR code for payment
     */
    @Transactional
    public QRGenerateResponse generateQRPayment(QRGenerateRequest request) {
        User currentUser = getCurrentUser();
        log.info("User {} generating QR payment for account: {}", 
                currentUser.getUsername(), request.getAccountNumber());

        // Validate and get account
        Account receiverAccount = accountRepository.findByAccountNumberWithCustomer(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.getAccountNumber()));

        // Security: Ensure user can only create QR for their own accounts
        if (!hasAccessToAccount(currentUser, receiverAccount)) {
            log.warn("User {} attempted to create QR for unauthorized account {}", 
                    currentUser.getUsername(), request.getAccountNumber());
            throw new SecurityException("You are not authorized to create QR for this account");
        }

        // Validate account is active
        if (!receiverAccount.isActive()) {
            throw new IllegalStateException("Account is not active: " + request.getAccountNumber());
        }

        try {
            // Calculate expiry
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(request.getExpiryHours());

            // Get receiver name
            String receiverName = receiverAccount.getCustomer().getFirstName() + " " + 
                                 receiverAccount.getCustomer().getLastName();

            // Create temporary request ID for QR code generation
            String tempRequestId = "QR" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));

            // Generate QR code with internal payment data format
            String qrCodeData = qrCodeService.generatePaymentQRCode(
                    tempRequestId,
                    receiverAccount.getAccountNumber(),
                    request.getAmount().toString(),
                    request.getDescription()
            );

            // Create QR payment request entity
            QRPaymentRequest qrRequest = QRPaymentRequest.builder()
                    .receiverAccount(receiverAccount)
                    .receiverName(receiverName)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .description(request.getDescription())
                    .status(QRPaymentRequest.STATUS_CREATED)
                    .qrCodeData(qrCodeData)
                    .qrType("DYNAMIC")
                    .createdBy(currentUser.getUsername())
                    .expiresAt(expiresAt)
                    .build();

            QRPaymentRequest savedRequest = qrPaymentRequestRepository.save(qrRequest);

            // Update the request ID in the QR code if needed (use the generated one from entity)
            if (!tempRequestId.equals(savedRequest.getRequestId())) {
                qrCodeData = qrCodeService.generatePaymentQRCode(
                        savedRequest.getRequestId(),
                        receiverAccount.getAccountNumber(),
                        request.getAmount().toString(),
                        request.getDescription()
                );
                savedRequest.setQrCodeData(qrCodeData);
                savedRequest = qrPaymentRequestRepository.save(savedRequest);
            }

            log.info("QR payment request created: {}", savedRequest.getRequestId());

            return QRGenerateResponse.success(
                    savedRequest.getId(),
                    savedRequest.getRequestId(),
                    receiverAccount.getAccountNumber(),
                    receiverName,
                    request.getAmount(),
                    request.getCurrency(),
                    request.getDescription(),
                    qrCodeData,
                    savedRequest.getCreatedAt(),
                    expiresAt
            );

        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR payment: " + e.getMessage(), e);
        }
    }

    /**
     * Parse uploaded QR code
     */
    @Transactional(readOnly = true)
    public QRParseResponse parseQRCode(QRParseRequest request) {
        try {
            log.info("Parsing QR code data");

            // Validate Base64 image
            if (!qrCodeService.isValidBase64Image(request.getQrCodeData())) {
                return QRParseResponse.invalid("Invalid QR code image data");
            }

            // Parse QR code
            Map<String, String> qrData = qrCodeService.parsePaymentQRCode(request.getQrCodeData());

            // Check if it's our internal format (BOP_PAY)
            if ("BOP".equals(qrData.get("type"))) {
                String requestId = qrData.get("requestId");
                
                if (requestId != null) {
                    QRPaymentRequest qrRequest = qrPaymentRequestRepository.findByRequestId(requestId)
                            .orElseThrow(() -> new IllegalArgumentException("QR payment request not found"));

                    // Check if expired
                    boolean expired = qrRequest.isExpired();
                    if (expired) {
                        return QRParseResponse.invalid("QR code has expired");
                    }

                    // Check if already paid
                    if (qrRequest.isPaid()) {
                        return QRParseResponse.invalid("QR code has already been used for payment");
                    }

                    return QRParseResponse.success(
                            qrRequest.getRequestId(),
                            qrRequest.getReceiverAccount().getAccountNumber(),
                            qrRequest.getReceiverName(),
                            qrRequest.getAmount(),
                            qrRequest.getCurrency(),
                            qrRequest.getDescription(),
                            qrRequest.getExpiresAt(),
                            expired
                    );
                }
            }

            return QRParseResponse.invalid("Unable to parse QR code payment data");

        } catch (NotFoundException e) {
            log.error("QR code not found in image: {}", e.getMessage());
            return QRParseResponse.invalid("No QR code found in the image");
        } catch (IOException e) {
            log.error("Failed to parse QR code: {}", e.getMessage(), e);
            return QRParseResponse.invalid("Failed to read QR code image");
        } catch (Exception e) {
            log.error("Error parsing QR code: {}", e.getMessage(), e);
            return QRParseResponse.invalid("Error parsing QR code: " + e.getMessage());
        }
    }

    /**
     * Process QR payment (internal transfer)
     */
    @Transactional
    public QRPayResponse processQRPayment(QRPayRequest request) {
        User currentUser = getCurrentUser();
        log.info("User {} processing QR payment for request: {}", 
                currentUser.getUsername(), request.getRequestId());

        try {
            // Find QR payment request
            QRPaymentRequest qrRequest = qrPaymentRequestRepository.findByRequestId(request.getRequestId())
                    .orElseThrow(() -> new IllegalArgumentException("QR payment request not found"));

            // Validate QR request
            if (qrRequest.isPaid()) {
                return QRPayResponse.failure("QR code has already been used for payment");
            }

            if (qrRequest.isExpired()) {
                return QRPayResponse.failure("QR code has expired");
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
                return QRPayResponse.failure("Payer account is not active");
            }

            // Validate sufficient balance
            if (payerAccount.getAvailableBalance().compareTo(qrRequest.getAmount()) < 0) {
                return QRPayResponse.failure("Insufficient balance. Available: " + 
                        payerAccount.getAvailableBalance() + ", Required: " + qrRequest.getAmount());
            }

            // Get receiver account
            Account receiverAccount = qrRequest.getReceiverAccount();

            // Cannot pay to same account
            if (payerAccount.getId().equals(receiverAccount.getId())) {
                return QRPayResponse.failure("Cannot pay to the same account");
            }

            // Process internal transfer
            LocalDateTime now = LocalDateTime.now();
            BigDecimal amount = qrRequest.getAmount();
            BigDecimal payerBalanceBefore = payerAccount.getBalance();

            // Debit from payer
            payerAccount.debit(amount);
            accountRepository.save(payerAccount);

            Transaction debitTransaction = Transaction.builder()
                    .account(payerAccount)
                    .destinationAccount(receiverAccount)
                    .transactionType(Transaction.TYPE_DEBIT)
                    .amount(amount)
                    .currency(qrRequest.getCurrency())
                    .balanceBefore(payerBalanceBefore)
                    .balanceAfter(payerAccount.getBalance())
                    .description("QR Payment to " + qrRequest.getReceiverName())
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
                    .currency(qrRequest.getCurrency())
                    .balanceBefore(receiverBalanceBefore)
                    .balanceAfter(receiverAccount.getBalance())
                    .description("QR Payment from " + currentUser.getUsername())
                    .category(Transaction.CATEGORY_PAYMENT)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(now)
                    .valueDate(now)
                    .initiatedBy(currentUser.getUsername())
                    .approvedBy("system")
                    .approvalDate(now)
                    .build();

            Transaction savedCreditTxn = transactionRepository.save(creditTransaction);

            // Create QR transaction record
            QRTransaction qrTransaction = QRTransaction.builder()
                    .paymentType(QRTransaction.TYPE_QR_CODE)
                    .qrRequest(qrRequest)
                    .payerAccount(payerAccount)
                    .receiverAccount(receiverAccount)
                    .amount(amount)
                    .netAmount(amount)
                    .currency(qrRequest.getCurrency())
                    .status(QRTransaction.STATUS_SETTLED)
                    .paymentMethod("INTERNAL")  // Internal bank transfer
                    .description(qrRequest.getDescription())
                    .initiatedBy(currentUser.getUsername())
                    .debitTransaction(savedDebitTxn)
                    .creditTransaction(savedCreditTxn)
                    .settledAt(now)
                    .build();

            QRTransaction savedQRTransaction = qrTransactionRepository.save(qrTransaction);

            // Update QR request status
            qrRequest.setStatus(QRPaymentRequest.STATUS_PAID);
            qrRequest.setPaidBy(currentUser.getUsername());
            qrRequest.setPayerAccount(payerAccount);
            qrRequest.setPaidAt(now);
            qrPaymentRequestRepository.save(qrRequest);

            log.info("QR payment completed successfully. Transaction: {}", 
                    savedQRTransaction.getTransactionReference());

            return QRPayResponse.success(
                    savedQRTransaction.getTransactionReference(),
                    qrRequest.getRequestId(),
                    payerAccount.getAccountNumber(),
                    receiverAccount.getAccountNumber(),
                    qrRequest.getReceiverName(),
                    amount,
                    qrRequest.getCurrency(),
                    payerBalanceBefore,
                    payerAccount.getBalance(),
                    now,
                    savedDebitTxn.getId(),
                    savedCreditTxn.getId()
            );

        } catch (Exception e) {
            log.error("Error processing QR payment: {}", e.getMessage(), e);
            return QRPayResponse.failure("Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * Get QR payment request by ID
     */
    @Transactional(readOnly = true)
    public QRGenerateResponse getQRPaymentRequest(String requestId) {
        QRPaymentRequest qrRequest = qrPaymentRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("QR payment request not found"));

        User currentUser = getCurrentUser();
        
        // Check access
        if (!hasAccessToAccount(currentUser, qrRequest.getReceiverAccount())) {
            throw new SecurityException("You don't have permission to view this QR request");
        }

        return mapToGenerateResponse(qrRequest);
    }

    /**
     * Get all QR payment requests for current user
     */
    @Transactional(readOnly = true)
    public List<QRGenerateResponse> getMyQRPaymentRequests() {
        User currentUser = getCurrentUser();
        
        List<QRPaymentRequest> requests = qrPaymentRequestRepository.findByCreatedBy(currentUser.getUsername());
        
        return requests.stream()
                .map(this::mapToGenerateResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get QR transaction history
     */
    @Transactional(readOnly = true)
    public List<QRTransactionResponse> getQRTransactionHistory(String accountNumber) {
        User currentUser = getCurrentUser();
        
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!hasAccessToAccount(currentUser, account)) {
            throw new SecurityException("You don't have permission to view transactions for this account");
        }

        List<QRTransaction> transactions = qrTransactionRepository.findByAccountId(account.getId());
        
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

    private QRGenerateResponse mapToGenerateResponse(QRPaymentRequest qrRequest) {
        return QRGenerateResponse.builder()
                .id(qrRequest.getId())
                .requestId(qrRequest.getRequestId())
                .accountNumber(qrRequest.getReceiverAccount().getAccountNumber())
                .receiverName(qrRequest.getReceiverName())
                .amount(qrRequest.getAmount())
                .currency(qrRequest.getCurrency())
                .description(qrRequest.getDescription())
                .status(qrRequest.getStatus())
                .qrCodeData(qrRequest.getQrCodeData())
                .createdAt(qrRequest.getCreatedAt())
                .expiresAt(qrRequest.getExpiresAt())
                .isExpired(qrRequest.isExpired())
                .build();
    }

    private QRTransactionResponse mapToTransactionResponse(QRTransaction qrTxn) {
        return QRTransactionResponse.builder()
                .id(qrTxn.getId())
                .transactionReference(qrTxn.getTransactionReference())
                .paymentType(qrTxn.getPaymentType())
                .payerAccountNumber(qrTxn.getPayerAccount().getAccountNumber())
                .receiverAccountNumber(qrTxn.getReceiverAccount().getAccountNumber())
                .receiverName(qrTxn.getReceiverAccount().getCustomer().getFirstName() + " " +
                             qrTxn.getReceiverAccount().getCustomer().getLastName())
                .amount(qrTxn.getAmount())
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

