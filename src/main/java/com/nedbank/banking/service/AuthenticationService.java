package com.nedbank.banking.service;

import com.nedbank.banking.config.JwtConfig;
import com.nedbank.banking.dto.LoginRequest;
import com.nedbank.banking.dto.LoginResponse;
import com.nedbank.banking.dto.RegisterRequest;
import com.nedbank.banking.entity.Role;
import com.nedbank.banking.entity.User;
import com.nedbank.banking.repository.RoleRepository;
import com.nedbank.banking.repository.UserRepository;
import com.nedbank.banking.security.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtConfig jwtConfig;

    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        try {
            // Find user by username, email, or mobile
            User user = findUserForLogin(loginRequest.getUsernameOrEmailOrMobile())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Authenticate user using actual username
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Generate JWT token
            String jwt = jwtTokenUtil.generateToken(user);

            logger.info("User {} authenticated successfully", user.getUsername());

            return LoginResponse.of(
                    jwt,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getMobile(),
                    user.getStatus(),
                    jwtConfig.getExpiration()
            );

        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsernameOrEmailOrMobile());
            throw new RuntimeException("Invalid username or password", e);
        }
    }

    private Optional<User> findUserForLogin(String usernameOrEmailOrMobile) {
        // Try to find by username first
        Optional<User> user = userRepository.findByUsername(usernameOrEmailOrMobile);
        if (user.isPresent()) {
            return user;
        }

        // Try to find by email
        user = userRepository.findByEmail(usernameOrEmailOrMobile);
        if (user.isPresent()) {
            return user;
        }

        // Try to find by mobile
        return userRepository.findByMobile(usernameOrEmailOrMobile);
    }

    public LoginResponse registerUser(RegisterRequest registerRequest) {
        // Check if username exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        // Check if email exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        // Check if mobile exists (if provided)
        if (registerRequest.getMobile() != null && !registerRequest.getMobile().trim().isEmpty()) {
            if (userRepository.existsByMobile(registerRequest.getMobile())) {
                throw new RuntimeException("Mobile number is already in use!");
            }
        }

        // Get default CUSTOMER role for bank customers
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Default CUSTOMER role not found"));

        // Create new user with PENDING_DETAILS status (needs to complete customer profile)
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .mobile(registerRequest.getMobile())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .status(User.STATUS_PENDING_DETAILS)
                .build();

        // Assign default CUSTOMER role
        user.addRole(customerRole);

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String jwt = jwtTokenUtil.generateToken(savedUser);

        logger.info("User {} registered successfully with status: {}. Customer details required to complete profile.", 
                   registerRequest.getUsername(), savedUser.getStatus());

        return LoginResponse.of(
                jwt,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getMobile(),
                savedUser.getStatus(),
                jwtConfig.getExpiration()
        );
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public boolean isMobileAvailable(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return true; // Empty mobile is considered available
        }
        return !userRepository.existsByMobile(mobile);
    }
}
