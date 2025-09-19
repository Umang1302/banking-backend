package com.nedbank.banking.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class LoginRequest {

    @NotBlank(message = "Email or Mobile is required")
    private String usernameOrEmailOrMobile;

    @NotBlank(message = "Password is required")
    private String password;
}
