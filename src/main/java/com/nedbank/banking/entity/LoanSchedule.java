package com.nedbank.banking.entity;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private LoanApplication loan;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "emi_amount", precision = 18, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "principal_component", precision = 18, scale = 2)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", precision = 18, scale = 2)
    private BigDecimal interestComponent;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";
}
