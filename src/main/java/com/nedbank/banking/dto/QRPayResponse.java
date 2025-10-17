package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for QR payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRPayResponse {

    private boolean success;
    private String message;
    private String transactionReference;
    private String qrRequestId;
    private String payerAccountNumber;
    private String receiverAccountNumber;
    private String receiverName;
    private BigDecimal amount;
    private String currency;
    private String status;
    private BigDecimal payerBalanceBefore;
    private BigDecimal payerBalanceAfter;
    private LocalDateTime paidAt;
    private Long debitTransactionId;
    private Long creditTransactionId;

    public static QRPayResponse success(
            String transactionReference,
            String qrRequestId,
            String payerAccountNumber,
            String receiverAccountNumber,
            String receiverName,
            BigDecimal amount,
            String currency,
            BigDecimal payerBalanceBefore,
            BigDecimal payerBalanceAfter,
            LocalDateTime paidAt,
            Long debitTransactionId,
            Long creditTransactionId
    ) {
        return QRPayResponse.builder()
                .success(true)
                .message("Payment successful")
                .transactionReference(transactionReference)
                .qrRequestId(qrRequestId)
                .payerAccountNumber(payerAccountNumber)
                .receiverAccountNumber(receiverAccountNumber)
                .receiverName(receiverName)
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

    public static QRPayResponse failure(String message) {
        return QRPayResponse.builder()
                .success(false)
                .message(message)
                .status("FAILED")
                .build();
    }
}

