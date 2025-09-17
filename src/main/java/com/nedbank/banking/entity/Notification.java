package com.nedbank.banking.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 20)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
