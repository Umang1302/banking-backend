package com.nedbank.banking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HealthResponse {
    private String status;
    private LocalDateTime timestamp;

    public boolean isHealthy() {
        return "UP".equalsIgnoreCase(status);
    }
}
