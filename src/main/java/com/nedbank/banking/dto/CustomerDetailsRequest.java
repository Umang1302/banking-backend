package com.nedbank.banking.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for collecting complete customer details during profile completion
 * This is used when users need to provide KYC information after initial registration
 */
@Data
public class CustomerDetailsRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name can only contain letters and spaces")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Last name can only contain letters and spaces")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDateTime dateOfBirth;

    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    private String address;

    @NotBlank(message = "National ID is required")
    @Size(min = 5, max = 50, message = "National ID must be between 5 and 50 characters")
    // @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "National ID can only contain letters and numbers")
    private String nationalId;

    // Optional fields - user might want to update these from registration
    @Size(max = 20, message = "Mobile number must be at most 20 characters")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid mobile number")
    private String mobile;

    @Size(max = 100, message = "Email must be at most 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Please provide a valid email address")
    private String email;

    // Additional personal information stored as JSON
    // Can include: salary, occupation, employer, maritalStatus, dependents, etc.
    private Map<String, Object> otherInfo;
}
