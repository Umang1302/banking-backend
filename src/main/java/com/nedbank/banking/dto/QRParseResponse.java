package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for QR code parsing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRParseResponse {

    private boolean valid;
    private String message;
    private String requestId;
    private String receiverAccountNumber;
    private String receiverName;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime expiresAt;
    private boolean expired;

    public static QRParseResponse success(
            String requestId,
            String receiverAccountNumber,
            String receiverName,
            BigDecimal amount,
            String currency,
            String description,
            LocalDateTime expiresAt,
            boolean expired
    ) {
        return QRParseResponse.builder()
                .valid(true)
                .message("QR code parsed successfully")
                .requestId(requestId)
                .receiverAccountNumber(receiverAccountNumber)
                .receiverName(receiverName)
                .amount(amount)
                .currency(currency)
                .description(description)
                .expiresAt(expiresAt)
                .expired(expired)
                .build();
    }

    public static QRParseResponse invalid(String message) {
        return QRParseResponse.builder()
                .valid(false)
                .message(message)
                .build();
    }
}

