package com.nedbank.banking.controller;

import com.nedbank.banking.dto.HealthResponse;
import com.nedbank.banking.service.ApplicationStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private ApplicationStatusService applicationStatusService;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse health = applicationStatusService.getApplicationHealth();
        
        if (health.isHealthy()) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(503).body(health);
        }
    }
}
