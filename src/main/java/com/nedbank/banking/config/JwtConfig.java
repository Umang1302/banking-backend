package com.nedbank.banking.config;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);

    private String secret;
    private long expiration;
    private long refreshExpiration;
    private String issuer;
    private String headerName;
    private String tokenPrefix;
    private long sessionTimeout;

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public String getTokenWithoutPrefix(String token) {
        if (token != null && token.startsWith(tokenPrefix)) {
            return token.substring(tokenPrefix.length());
        }
        return token;
    }

    @PostConstruct
    public void logConfiguration() {
        logger.info("=== JWT Configuration Loaded ===");
        logger.info("Issuer: {}", issuer);
        logger.info("Expiration: {} ms ({} hours)", expiration, expiration / (1000 * 60 * 60));
        logger.info("Session Timeout: {} ms ({} minutes)", sessionTimeout, sessionTimeout / (1000 * 60));
        logger.info("Header Name: {}", headerName);
        logger.info("Token Prefix: '{}'", tokenPrefix);
        logger.info("Secret Length: {} characters", secret != null ? secret.length() : 0);
        logger.info("Refresh Expiration: {} ms ({} days)", refreshExpiration, refreshExpiration / (1000 * 60 * 60 * 24));
        logger.info("================================");
    }
}
