package com.nedbank.banking.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Column(length = 100)
    private String action;

    @Column(length = 50)
    private String entity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "old_data_json", columnDefinition = "JSONB")
    private String oldDataJson;

    @Column(name = "new_data_json", columnDefinition = "JSONB")
    private String newDataJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
