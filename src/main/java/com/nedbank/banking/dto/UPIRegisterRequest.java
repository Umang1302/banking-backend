package com.nedbank.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering a UPI ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UPIRegisterRequest {

    @NotBlank(message = "UPI ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$", 
             message = "Invalid UPI ID format. Expected format: user@provider")
    private String upiId;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @Builder.Default
    private boolean isPrimary = false;
}

