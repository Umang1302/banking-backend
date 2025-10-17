package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for UPI ID registration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UPIRegisterResponse {

    private boolean success;
    private String message;
    private Long id;
    private String upiId;
    private String accountNumber;
    private String upiProvider;
    private boolean isPrimary;
    private String status;
    private LocalDateTime createdAt;

    public static UPIRegisterResponse success(
            Long id,
            String upiId,
            String accountNumber,
            String upiProvider,
            boolean isPrimary,
            String status,
            LocalDateTime createdAt
    ) {
        return UPIRegisterResponse.builder()
                .success(true)
                .message("UPI ID registered successfully")
                .id(id)
                .upiId(upiId)
                .accountNumber(accountNumber)
                .upiProvider(upiProvider)
                .isPrimary(isPrimary)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    public static UPIRegisterResponse failure(String message) {
        return UPIRegisterResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}

