package com.nedbank.banking.controller;

import com.nedbank.banking.dto.LoginRequest;
import com.nedbank.banking.dto.LoginResponse;
import com.nedbank.banking.dto.RegisterRequest;
import com.nedbank.banking.service.AuthenticationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authenticationService.authenticateUser(loginRequest);
            
            logger.info("User {} logged in successfully", loginRequest.getUsernameOrEmailOrMobile());
            
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) {
            logger.error("Login failed for user {}: {}", loginRequest.getUsernameOrEmailOrMobile(), e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid username or password");
            error.put("status", "error");
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            LoginResponse loginResponse = authenticationService.registerUser(registerRequest);
            
            logger.info("User {} registered successfully", registerRequest.getUsername());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(loginResponse);
        } catch (Exception e) {
            logger.error("Registration failed for user {}: {}", registerRequest.getUsername(), e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsernameAvailability(@PathVariable String username) {
        boolean isAvailable = authenticationService.isUsernameAvailable(username);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailability(@PathVariable String email) {
        boolean isAvailable = authenticationService.isEmailAvailable(email);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-mobile/{mobile}")
    public ResponseEntity<Map<String, Boolean>> checkMobileAvailability(@PathVariable String mobile) {
        boolean isAvailable = authenticationService.isMobileAvailable(mobile);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);
        
        return ResponseEntity.ok(response);
    }
}
