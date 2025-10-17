package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for beneficiary details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryResponse {

    private Long id;
    private String beneficiaryName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String branchName;
    private String nickname;
    private String email;
    private String mobile;
    private Boolean isVerified;
    private String status;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Masked account number for display
    private String maskedAccountNumber;

    public String getMaskedAccountNumber() {
        if (accountNumber != null && accountNumber.length() > 4) {
            return "XXXX" + accountNumber.substring(accountNumber.length() - 4);
        }
        return accountNumber;
    }
}

