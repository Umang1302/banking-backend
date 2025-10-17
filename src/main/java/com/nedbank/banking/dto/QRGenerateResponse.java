package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for QR code generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRGenerateResponse {

    private Long id;
    private String requestId;
    private String accountNumber;
    private String receiverName;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String status;
    private String qrCodeData;  // Base64 encoded image
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isExpired;

    public static QRGenerateResponse success(
            Long id,
            String requestId,
            String accountNumber,
            String receiverName,
            BigDecimal amount,
            String currency,
            String description,
            String qrCodeData,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        return QRGenerateResponse.builder()
                .id(id)
                .requestId(requestId)
                .accountNumber(accountNumber)
                .receiverName(receiverName)
                .amount(amount)
                .currency(currency)
                .description(description)
                .status("CREATED")
                .qrCodeData(qrCodeData)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .isExpired(false)
                .build();
    }
}

