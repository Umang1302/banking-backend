package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for checking EFT transaction status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EFTStatusResponse {

    private Long eftTransactionId;
    private String eftReference;
    private String eftType;
    private String status;
    private String fromAccountNumber;
    private String beneficiaryAccountNumber;
    private String beneficiaryName;
    private String beneficiaryBank;
    private BigDecimal amount;
    private BigDecimal charges;
    private BigDecimal totalAmount;
    private String currency;
    private String purpose;
    private String batchId;
    private LocalTime batchTime;
    private LocalDateTime estimatedCompletion;
    private LocalDateTime actualCompletion;
    private String failureReason;
    private LocalDateTime initiatedAt;
    private LocalDateTime updatedAt;
}

