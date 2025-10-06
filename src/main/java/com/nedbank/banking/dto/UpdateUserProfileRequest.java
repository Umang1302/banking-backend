package com.nedbank.banking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    
    @Email(message = "Email should be valid")
    private String email;
    
    @Size(max = 20, message = "Mobile number should not exceed 20 characters")
    private String mobile;

}
