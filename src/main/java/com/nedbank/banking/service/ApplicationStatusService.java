package com.nedbank.banking.service;

import com.nedbank.banking.dto.HealthResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ApplicationStatusService {

    public HealthResponse getApplicationHealth() {
        // If this method is called, the application is running
        return HealthResponse.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
