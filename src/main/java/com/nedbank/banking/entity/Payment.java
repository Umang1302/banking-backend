package com.nedbank.banking.entity;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(length = 20)
    private String channel;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "payee_details", length = 255)
    private String payeeDetails;

    @Column(length = 100)
    private String reference;

    @Column(length = 20)
    @Builder.Default
    private String status = "INITIATED";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
