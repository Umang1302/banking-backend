package com.nedbank.banking.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_uid", nullable = false, unique = true, length = 50)
    private String customerUid;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 20)
    private String mobile;

    @Column(name = "kyc_status", length = 20)
    @Builder.Default
    private String kycStatus = "PENDING";

    @Column(name = "customer_type", length = 20)
    @Builder.Default
    private String customerType = "INDIVIDUAL";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
