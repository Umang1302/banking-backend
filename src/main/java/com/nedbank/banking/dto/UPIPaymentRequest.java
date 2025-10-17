package com.nedbank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for UPI payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UPIPaymentRequest {

    @NotBlank(message = "Receiver UPI ID is required")
    private String receiverUpiId;

    @NotBlank(message = "Payer account number is required")
    private String payerAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least 1.0")
    @DecimalMax(value = "1000000.0", message = "Amount cannot exceed 1,000,000")
    private BigDecimal amount;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Builder.Default
    private String currency = "INR";
}

