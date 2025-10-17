package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for RTGS transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RTGSTransferResponse {

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
    private LocalDateTime processedAt;
    private LocalDateTime initiatedAt;
    private String message;

    public static RTGSTransferResponse success(
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
            LocalDateTime processedAt,
            LocalDateTime initiatedAt) {
        
        return RTGSTransferResponse.builder()
                .eftTransactionId(eftTransactionId)
                .eftReference(eftReference)
                .status("COMPLETED")
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
                .processedAt(processedAt)
                .initiatedAt(initiatedAt)
                .message("RTGS transfer completed successfully. Amount has been transferred in real-time.")
                .build();
    }

    public static RTGSTransferResponse failed(
            Long eftTransactionId,
            String eftReference,
            String fromAccountNumber,
            String beneficiaryAccountNumber,
            String beneficiaryName,
            BigDecimal amount,
            String failureReason,
            LocalDateTime initiatedAt) {
        
        return RTGSTransferResponse.builder()
                .eftTransactionId(eftTransactionId)
                .eftReference(eftReference)
                .status("FAILED")
                .fromAccountNumber(fromAccountNumber)
                .beneficiaryAccountNumber(beneficiaryAccountNumber)
                .beneficiaryName(beneficiaryName)
                .amount(amount)
                .initiatedAt(initiatedAt)
                .message("RTGS transfer failed: " + failureReason)
                .build();
    }
}

