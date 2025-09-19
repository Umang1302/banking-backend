package com.nedbank.banking.service;

import com.nedbank.banking.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;

@Service
public class ApplicationStatusService {

    @Autowired
    private DataSource dataSource;

    public HealthResponse getApplicationHealth() {
        String status = "UP";
        
        try {
            // Check database connectivity
            try (Connection connection = dataSource.getConnection()) {
                if (connection == null || !connection.isValid(5)) {
                    status = "DOWN";
                }
            }
        } catch (Exception e) {
            status = "DOWN";
        }
        
        return HealthResponse.builder()
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
