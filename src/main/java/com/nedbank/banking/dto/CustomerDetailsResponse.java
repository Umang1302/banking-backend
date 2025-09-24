package com.nedbank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for responding after customer details submission
 * Provides feedback to user about their application status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetailsResponse {

    private Long userId;
    private Long customerId;
    private String username;
    private String customerNumber;
    private String userStatus;
    private String customerStatus;
    private String message;
    private LocalDateTime submittedAt;

    // Customer details for confirmation
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private LocalDateTime dateOfBirth;
    private String address;
    private String nationalId;

    /**
     * Static factory method for successful submission
     */
    public static CustomerDetailsResponse success(Long userId, Long customerId, String username, 
                                                String customerNumber, String firstName, String lastName,
                                                String email, String mobile, LocalDateTime dateOfBirth,
                                                String address, String nationalId) {
        return CustomerDetailsResponse.builder()
                .userId(userId)
                .customerId(customerId)
                .username(username)
                .customerNumber(customerNumber)
                .userStatus("PENDING_REVIEW")
                .customerStatus("PENDING_REVIEW")
                .message("Customer details submitted successfully. Your application is now under review by our team.")
                .submittedAt(LocalDateTime.now())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .mobile(mobile)
                .dateOfBirth(dateOfBirth)
                .address(address)
                .nationalId(nationalId)
                .build();
    }

    /**
     * Static factory method for validation errors
     */
    public static CustomerDetailsResponse error(String message) {
        return CustomerDetailsResponse.builder()
                .message(message)
                .submittedAt(LocalDateTime.now())
                .build();
    }
}
