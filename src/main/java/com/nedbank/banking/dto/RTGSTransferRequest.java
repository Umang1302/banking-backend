package com.nedbank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for RTGS transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RTGSTransferRequest {

    @NotBlank(message = "Source account number is required")
    private String fromAccountNumber;

    @NotNull(message = "Beneficiary ID is required")
    private Long beneficiaryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "200000.00", message = "RTGS requires minimum amount of ₹2,00,000")
    @DecimalMax(value = "1000000000.00", message = "Amount cannot exceed ₹100,00,00,000 for RTGS")
    private BigDecimal amount;

    @NotBlank(message = "Purpose is required")
    @Size(min = 5, max = 255, message = "Purpose must be between 5 and 255 characters")
    private String purpose;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}

