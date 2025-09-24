package com.nedbank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(exclude = {"roles", "customer"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    // User Status Constants
    public static final String STATUS_PENDING_DETAILS = "PENDING_DETAILS";
    public static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true, nullable = false)
    private String username;

    @Column(length = 100, unique = true, nullable = false)
    private String email;

    @Column(length = 20, unique = true)
    private String mobile;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String password;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = STATUS_PENDING_DETAILS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add role authorities
        roles.forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            
            // Add permission authorities
            role.getPermissions().forEach(permission -> {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            });
        });
        
        return authorities;
    }

    // Helper methods for role management
    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.getName().equals(roleName));
    }

    public boolean hasPermission(String permissionName) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    // Helper methods for status management
    public boolean needsCustomerDetails() {
        return STATUS_PENDING_DETAILS.equals(status);
    }

    public boolean isPendingReview() {
        return STATUS_PENDING_REVIEW.equals(status);
    }

    public boolean isUnderReview() {
        return STATUS_UNDER_REVIEW.equals(status);
    }

    public boolean isFullyActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }

    public boolean isSuspended() {
        return STATUS_SUSPENDED.equals(status);
    }

    public boolean canAccessBankingFeatures() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean canLogin() {
        return Arrays.asList(STATUS_PENDING_DETAILS, STATUS_PENDING_REVIEW, 
                           STATUS_UNDER_REVIEW, STATUS_ACTIVE).contains(status);
    }

    // UserDetails interface methods
    @Override
    public boolean isAccountNonExpired() {
        return !STATUS_EXPIRED.equals(status);
    }

    @Override
    public boolean isAccountNonLocked() {
        return !STATUS_LOCKED.equals(status) && !STATUS_SUSPENDED.equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return canLogin();
    }
}
