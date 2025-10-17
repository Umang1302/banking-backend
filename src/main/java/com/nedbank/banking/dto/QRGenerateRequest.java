package com.nedbank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for generating a QR code payment request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRGenerateRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least 1.0")
    @DecimalMax(value = "1000000.0", message = "Amount cannot exceed 1,000,000")
    private BigDecimal amount;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Builder.Default
    private String currency = "INR";

    @Min(value = 1, message = "Expiry hours must be at least 1")
    @Max(value = 168, message = "Expiry hours cannot exceed 168 (7 days)")
    @Builder.Default
    private Integer expiryHours = 24;  // Default 24 hours
}

