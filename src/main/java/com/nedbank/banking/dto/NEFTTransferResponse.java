package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for NEFT transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NEFTTransferResponse {

    private Long eftTransactionId;
    private String eftReference;
    private String status;
    private String fromAccountNumber;
    private String beneficiaryAccountNumber;
    private String beneficiaryName;
    private String beneficiaryBank;
    private String beneficiaryIfsc;
    private BigDecimal amount;
    private BigDecimal charges;
    private BigDecimal totalAmount;
    private String currency;
    private String purpose;
    private String batchId;
    private LocalTime nextBatchTime;
    private LocalDateTime estimatedCompletion;
    private LocalDateTime initiatedAt;
    private String message;

    public static NEFTTransferResponse success(
            Long eftTransactionId,
            String eftReference,
            String fromAccountNumber,
            String beneficiaryAccountNumber,
            String beneficiaryName,
            String beneficiaryBank,
            String beneficiaryIfsc,
            BigDecimal amount,
            BigDecimal charges,
            String purpose,
            LocalTime nextBatchTime,
            LocalDateTime estimatedCompletion,
            LocalDateTime initiatedAt) {
        
        return NEFTTransferResponse.builder()
                .eftTransactionId(eftTransactionId)
                .eftReference(eftReference)
                .status("PENDING")
                .fromAccountNumber(fromAccountNumber)
                .beneficiaryAccountNumber(beneficiaryAccountNumber)
                .beneficiaryName(beneficiaryName)
                .beneficiaryBank(beneficiaryBank)
                .beneficiaryIfsc(beneficiaryIfsc)
                .amount(amount)
                .charges(charges)
                .totalAmount(amount.add(charges))
                .currency("INR")
                .purpose(purpose)
                .nextBatchTime(nextBatchTime)
                .estimatedCompletion(estimatedCompletion)
                .initiatedAt(initiatedAt)
                .message("NEFT transfer initiated successfully. Transaction will be processed in the next batch at " + nextBatchTime)
                .build();
    }
}

