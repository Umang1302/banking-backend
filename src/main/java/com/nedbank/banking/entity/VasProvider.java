package com.nedbank.banking.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "vas_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VasProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "service_type", length = 50)
    private String serviceType;

    @Column(name = "api_url", length = 255)
    private String apiUrl;

    @Column
    @Builder.Default
    private Boolean active = true;
}
