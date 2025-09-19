package com.nedbank.banking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String type;
    private Long userId;
    private String username;
    private String email;
    private String mobile;
    private String status;
    private long expiresIn;

    public static LoginResponse of(String token, Long userId, String username, 
                                  String email, String mobile, String status, long expiresIn) {
        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(userId)
                .username(username)
                .email(email)
                .mobile(mobile)
                .status(status)
                .expiresIn(expiresIn)
                .build();
    }
}
