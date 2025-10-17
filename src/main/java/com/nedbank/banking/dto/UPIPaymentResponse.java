package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for UPI payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UPIPaymentResponse {

    private boolean success;
    private String message;
    private String transactionReference;
    private String receiverUpiId;
    private String receiverAccountNumber;
    private String receiverName;
    private String payerAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String status;
    private BigDecimal payerBalanceBefore;
    private BigDecimal payerBalanceAfter;
    private LocalDateTime paidAt;
    private Long debitTransactionId;
    private Long creditTransactionId;

    public static UPIPaymentResponse success(
            String transactionReference,
            String receiverUpiId,
            String receiverAccountNumber,
            String receiverName,
            String payerAccountNumber,
            BigDecimal amount,
            String currency,
            BigDecimal payerBalanceBefore,
            BigDecimal payerBalanceAfter,
            LocalDateTime paidAt,
            Long debitTransactionId,
            Long creditTransactionId
    ) {
        return UPIPaymentResponse.builder()
                .success(true)
                .message("UPI payment completed successfully")
                .transactionReference(transactionReference)
                .receiverUpiId(receiverUpiId)
                .receiverAccountNumber(receiverAccountNumber)
                .receiverName(receiverName)
                .payerAccountNumber(payerAccountNumber)
                .amount(amount)
                .currency(currency)
                .status("SETTLED")
                .payerBalanceBefore(payerBalanceBefore)
                .payerBalanceAfter(payerBalanceAfter)
                .paidAt(paidAt)
                .debitTransactionId(debitTransactionId)
                .creditTransactionId(creditTransactionId)
                .build();
    }

    public static UPIPaymentResponse failure(String message) {
        return UPIPaymentResponse.builder()
                .success(false)
                .message(message)
                .status("FAILED")
                .build();
    }
}

