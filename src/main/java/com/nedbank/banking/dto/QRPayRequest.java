package com.nedbank.banking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for paying via QR code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRPayRequest {

    @NotBlank(message = "QR request ID is required")
    private String requestId;

    @NotBlank(message = "Payer account number is required")
    private String payerAccountNumber;
}

