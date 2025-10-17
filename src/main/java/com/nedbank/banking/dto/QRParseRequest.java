package com.nedbank.banking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for parsing/uploading QR code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRParseRequest {

    @NotBlank(message = "QR code data is required")
    private String qrCodeData;  // Base64 encoded image or payment URL
}

