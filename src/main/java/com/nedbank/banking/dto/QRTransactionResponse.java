package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for QR/UPI transaction details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRTransactionResponse {

    private Long id;
    private String transactionReference;
    private String paymentType;  // QR_CODE or UPI
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String payerAccountNumber;
    private String receiverAccountNumber;
    private String receiverName;
    private BigDecimal amount;
    private BigDecimal razorpayFee;
    private BigDecimal netAmount;
    private String currency;
    private String status;
    private String paymentMethod;  // UPI, CARD, NETBANKING, WALLET
    private String description;
    private String initiatedBy;
    private String failureReason;
    private LocalDateTime initiatedAt;
    private LocalDateTime settledAt;
    private Long debitTransactionId;
    private Long creditTransactionId;
}

