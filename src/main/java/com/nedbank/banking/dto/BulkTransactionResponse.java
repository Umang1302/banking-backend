package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTransactionResponse {

    private String batchId;
    private int totalTransactions;
    private int successfulTransactions;
    private int failedTransactions;
    private List<TransactionResponse> successfulResults;
    private List<BulkTransactionError> errors;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkTransactionError {
        private int rowNumber;
        private String accountNumber;
        private String error;
    }
}

