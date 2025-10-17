package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Response DTO for NEFT batch information (Admin dashboard)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NEFTBatchResponse {

    private String batchId;
    private LocalTime batchTime;
    private LocalDateTime processedAt;
    private Integer totalTransactions;
    private Integer successfulTransactions;
    private Integer failedTransactions;
    private BigDecimal totalAmount;
    private String status;
    private List<EFTStatusResponse> transactions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSummary {
        private String batchId;
        private LocalTime batchTime;
        private Integer transactionCount;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime processedAt;
    }
}

