package com.nedbank.banking.entity;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_batch_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionBatchItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private TransactionBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "txn_type", length = 10)
    private String txnType;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;
}
