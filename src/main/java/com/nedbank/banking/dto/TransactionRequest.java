package com.nedbank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "DEBIT|CREDIT|TRANSFER|WITHDRAWAL|DEPOSIT", 
             message = "Transaction type must be one of: DEBIT, CREDIT, TRANSFER, WITHDRAWAL, DEPOSIT")
    private String transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    private String destinationAccountNumber;  // For transfers

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Pattern(regexp = "PAYMENT|TRANSFER|WITHDRAWAL|DEPOSIT|BILL_PAYMENT|SALARY|REFUND|OTHER", 
             message = "Invalid category")
    private String category;

    private String referenceNumber;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency;
}

