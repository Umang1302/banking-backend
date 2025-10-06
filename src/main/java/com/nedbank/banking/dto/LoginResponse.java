package com.nedbank.banking.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.nedbank.banking.entity.Role;

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
    private String role;  // Single role name (first role)
    private Set<String> permissions;  // User permissions for frontend
    private long expiresIn;

    public static LoginResponse of(String token, Long userId, String username, 
                                  String email, String mobile, String status, Set<Role> roles, long expiresIn) {
        // Get first role name
        String roleName = roles.stream()
                .findFirst()
                .map(Role::getName)
                .orElse("CUSTOMER");
        
        // Extract all permissions from all roles
        Set<String> permissions = roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());
        
        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(userId)
                .username(username)
                .email(email)
                .mobile(mobile)
                .status(status)
                .role(roleName)
                .permissions(permissions)
                .expiresIn(expiresIn)
                .build();
    }
}
