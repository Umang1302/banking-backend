package com.nedbank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for NEFT transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NEFTTransferRequest {

    @NotBlank(message = "Source account number is required")
    private String fromAccountNumber;

    @NotNull(message = "Beneficiary ID is required")
    private Long beneficiaryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least ₹1")
    @DecimalMax(value = "10000000.00", message = "Amount cannot exceed ₹1,00,00,000 for NEFT")
    private BigDecimal amount;

    @NotBlank(message = "Purpose is required")
    @Size(min = 5, max = 255, message = "Purpose must be between 5 and 255 characters")
    private String purpose;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}

