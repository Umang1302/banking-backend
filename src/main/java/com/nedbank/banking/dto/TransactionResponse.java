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
public class TransactionResponse {

    private Long transactionId;
    private String transactionReference;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String accountNumber;
    private String destinationAccountNumber;
    private String description;
    private String category;
    private String status;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private LocalDateTime transactionDate;
    private String initiatedBy;
    private String referenceNumber;
    private String failureReason;

    public static TransactionResponse success(Long transactionId, String transactionReference,
                                             String transactionType, BigDecimal amount, String currency,
                                             String accountNumber, String destinationAccountNumber,
                                             String description, String category, String status,
                                             BigDecimal balanceBefore, BigDecimal balanceAfter,
                                             LocalDateTime transactionDate, String initiatedBy) {
        return TransactionResponse.builder()
                .transactionId(transactionId)
                .transactionReference(transactionReference)
                .transactionType(transactionType)
                .amount(amount)
                .currency(currency)
                .accountNumber(accountNumber)
                .destinationAccountNumber(destinationAccountNumber)
                .description(description)
                .category(category)
                .status(status)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .transactionDate(transactionDate)
                .initiatedBy(initiatedBy)
                .build();
    }
}

