package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyTransferResponse {
    
    private String transactionReference;
    private String status;
    private String message;
    
    // Sender details
    private String fromAccountNumber;
    private BigDecimal senderBalanceBefore;
    private BigDecimal senderBalanceAfter;
    
    // Recipient details
    private String toAccountNumber;
    private String recipientName;
    
    // Transfer details
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime transactionDate;
    
    // Transaction IDs for tracking
    private Long debitTransactionId;
    private Long creditTransactionId;
    
    public static MoneyTransferResponse success(String transactionRef, String fromAccount, 
                                               String toAccount, String recipientName,
                                               BigDecimal amount, String currency,
                                               BigDecimal senderBalanceBefore, BigDecimal senderBalanceAfter,
                                               String description, LocalDateTime transactionDate,
                                               Long debitTxnId, Long creditTxnId) {
        return MoneyTransferResponse.builder()
                .transactionReference(transactionRef)
                .status("SUCCESS")
                .message("Money transferred successfully")
                .fromAccountNumber(fromAccount)
                .toAccountNumber(toAccount)
                .recipientName(recipientName)
                .amount(amount)
                .currency(currency)
                .senderBalanceBefore(senderBalanceBefore)
                .senderBalanceAfter(senderBalanceAfter)
                .description(description)
                .transactionDate(transactionDate)
                .debitTransactionId(debitTxnId)
                .creditTransactionId(creditTxnId)
                .build();
    }
    
    public static MoneyTransferResponse failure(String message) {
        return MoneyTransferResponse.builder()
                .status("FAILED")
                .message(message)
                .build();
    }
}

