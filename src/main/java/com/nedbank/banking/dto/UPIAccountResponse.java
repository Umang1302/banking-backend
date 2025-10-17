package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for UPI Account details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UPIAccountResponse {

    private Long id;
    private String upiId;
    private String accountNumber;
    private String upiProvider;
    private boolean isPrimary;
    private String status;
    private boolean isVerified;
    private LocalDateTime verifiedAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}

