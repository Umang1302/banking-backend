package com.nedbank.banking.config;

import com.nedbank.banking.entity.*;
import com.nedbank.banking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    @Transactional
    public void seedDatabase() {
        log.info("Starting database seeding...");
        
        // Check if any data already exists
        if (roleRepository.count() > 0 || permissionRepository.count() > 0 || 
            customerRepository.count() > 0 || userRepository.count() > 0) {
            log.info("Database already contains data. Skipping seeding.");
            logExistingDataSummary();
            return;
        }

        seedPermissions();
        seedRoles();
        seedCustomersAndAccounts();
        seedUsers();
        
        log.info("Database seeding completed successfully!");
    }

    private void seedPermissions() {
        log.info("Seeding permissions...");
        
        List<Permission> permissions = Arrays.asList(
            // User Management
            Permission.of("USER_READ", "View user information"),
            Permission.of("USER_WRITE", "Create and update users"),
            Permission.of("USER_DELETE", "Delete users"),
            Permission.of("USER_MANAGE_ROLES", "Assign roles to users"),
            
            // Customer Management
            Permission.of("CUSTOMER_READ", "View customer information"),
            Permission.of("CUSTOMER_WRITE", "Create and update customers"),
            Permission.of("CUSTOMER_DELETE", "Delete customers"),
            
            // Account Management
            Permission.of("ACCOUNT_READ", "View account information"),
            Permission.of("ACCOUNT_WRITE", "Create and update accounts"),
            Permission.of("ACCOUNT_DELETE", "Delete accounts"),
            Permission.of("ACCOUNT_BALANCE_VIEW", "View account balances"),
            
            // Transaction Management
            Permission.of("TRANSACTION_READ", "View transactions"),
            Permission.of("TRANSACTION_WRITE", "Create transactions"),
            Permission.of("TRANSACTION_APPROVE", "Approve large transactions"),
            
            // Report Access
            Permission.of("REPORTS_BASIC", "Access basic reports"),
            Permission.of("REPORTS_ADVANCED", "Access advanced reports"),
            Permission.of("REPORTS_FINANCIAL", "Access financial reports"),
            
            // Dashboard Access
            Permission.of("DASHBOARD_CUSTOMER", "Access customer dashboard"),
            Permission.of("DASHBOARD_ACCOUNTANT", "Access accountant dashboard"),
            Permission.of("DASHBOARD_ADMIN", "Access admin dashboard"),
            Permission.of("DASHBOARD_SUPERADMIN", "Access superadmin dashboard"),
            
            // System Management
            Permission.of("SYSTEM_CONFIG", "Manage system configuration"),
            Permission.of("AUDIT_LOGS", "View audit logs"),
            Permission.of("SYSTEM_BACKUP", "Perform system backups")
        );

        permissions.forEach(permission -> {
            if (!permissionRepository.existsByName(permission.getName())) {
                permissionRepository.save(permission);
                log.debug("Created permission: {}", permission.getName());
            }
        });
        
        log.info("Permissions seeded: {} total", permissions.size());
    }

    private void seedRoles() {
        log.info("Seeding roles...");
        
        // Customer Role
        if (!roleRepository.existsByName("CUSTOMER")) {
            Role customerRole = Role.builder()
                    .name("CUSTOMER")
                    .description("Bank customer with basic account access")
                    .build();
            
            // Add customer permissions
            List<String> customerPermissions = Arrays.asList(
                "ACCOUNT_READ", "ACCOUNT_BALANCE_VIEW", "TRANSACTION_READ",
                "DASHBOARD_CUSTOMER", "REPORTS_BASIC"
            );
            
            customerPermissions.forEach(permName -> {
                permissionRepository.findByName(permName)
                        .ifPresent(customerRole::addPermission);
            });
            
            roleRepository.save(customerRole);
            log.info("Created role: CUSTOMER");
        }
        
        // Accountant Role
        if (!roleRepository.existsByName("ACCOUNTANT")) {
            Role accountantRole = Role.builder()
                    .name("ACCOUNTANT")
                    .description("Bank accountant with transaction and report access")
                    .build();
            
            List<String> accountantPermissions = Arrays.asList(
                "CUSTOMER_READ", "ACCOUNT_READ", "ACCOUNT_WRITE", "ACCOUNT_BALANCE_VIEW",
                "TRANSACTION_READ", "TRANSACTION_WRITE", "REPORTS_BASIC", "REPORTS_ADVANCED",
                "DASHBOARD_ACCOUNTANT"
            );
            
            accountantPermissions.forEach(permName -> {
                permissionRepository.findByName(permName)
                        .ifPresent(accountantRole::addPermission);
            });
            
            roleRepository.save(accountantRole);
            log.info("Created role: ACCOUNTANT");
        }
        
        // Admin Role
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .description("Bank administrator with user and system management")
                    .build();
            
            List<String> adminPermissions = Arrays.asList(
                "USER_READ", "USER_WRITE", "CUSTOMER_READ", "CUSTOMER_WRITE",
                "ACCOUNT_READ", "ACCOUNT_WRITE", "ACCOUNT_DELETE", "ACCOUNT_BALANCE_VIEW",
                "TRANSACTION_READ", "TRANSACTION_WRITE", "TRANSACTION_APPROVE",
                "REPORTS_BASIC", "REPORTS_ADVANCED", "REPORTS_FINANCIAL",
                "DASHBOARD_ADMIN", "AUDIT_LOGS"
            );
            
            adminPermissions.forEach(permName -> {
                permissionRepository.findByName(permName)
                        .ifPresent(adminRole::addPermission);
            });
            
            roleRepository.save(adminRole);
            log.info("Created role: ADMIN");
        }
        
        // Superadmin Role
        if (!roleRepository.existsByName("SUPERADMIN")) {
            Role superadminRole = Role.builder()
                    .name("SUPERADMIN")
                    .description("System superadmin with full access")
                    .build();
            
            // Superadmin gets all permissions
            List<Permission> allPermissions = permissionRepository.findAll();
            allPermissions.forEach(superadminRole::addPermission);
            
            roleRepository.save(superadminRole);
            log.info("Created role: SUPERADMIN with {} permissions", allPermissions.size());
        }
        
        log.info("Roles seeded: 4 roles created");
    }

    private void seedCustomersAndAccounts() {
        log.info("Seeding customers and accounts...");
        
        // Customer 1 - John Doe
        Customer customer1 = Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .mobile("+1234567890")
                .address("123 Main Street, New York, NY 10001")
                .nationalId("123456789")
                .dateOfBirth(LocalDateTime.of(1985, 5, 15, 0, 0))
                .build();
        customer1 = customerRepository.save(customer1);
        
        // John's accounts
        Account johnSavings = Account.builder()
                .accountType("SAVINGS")
                .balance(new BigDecimal("15000.00"))
                .availableBalance(new BigDecimal("15000.00"))
                .currency("USD")
                .interestRate(new BigDecimal("2.5"))
                .minimumBalance(new BigDecimal("1000.00"))
                .customer(customer1)
                .build();
        
        Account johnCurrent = Account.builder()
                .accountType("CURRENT")
                .balance(new BigDecimal("5500.75"))
                .availableBalance(new BigDecimal("5500.75"))
                .currency("USD")
                .minimumBalance(new BigDecimal("500.00"))
                .customer(customer1)
                .build();
        
        accountRepository.saveAll(Arrays.asList(johnSavings, johnCurrent));
        
        // Customer 2 - Jane Smith
        Customer customer2 = Customer.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .mobile("+1234567891")
                .address("456 Oak Avenue, Los Angeles, CA 90210")
                .nationalId("987654321")
                .dateOfBirth(LocalDateTime.of(1990, 8, 22, 0, 0))
                .build();
        customer2 = customerRepository.save(customer2);
        
        // Jane's accounts
        Account janeSavings = Account.builder()
                .accountType("SAVINGS")
                .balance(new BigDecimal("25000.00"))
                .availableBalance(new BigDecimal("25000.00"))
                .currency("USD")
                .interestRate(new BigDecimal("2.75"))
                .minimumBalance(new BigDecimal("1000.00"))
                .customer(customer2)
                .build();
        
        Account janeFixed = Account.builder()
                .accountType("FIXED_DEPOSIT")
                .balance(new BigDecimal("50000.00"))
                .availableBalance(new BigDecimal("50000.00"))
                .currency("USD")
                .interestRate(new BigDecimal("4.5"))
                .minimumBalance(new BigDecimal("10000.00"))
                .customer(customer2)
                .build();
        
        accountRepository.saveAll(Arrays.asList(janeSavings, janeFixed));
        
        // Customer 3 - Bob Johnson (Business Customer)
        Customer customer3 = Customer.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@business.com")
                .mobile("+1234567892")
                .address("789 Business Park, Chicago, IL 60601")
                .nationalId("456789123")
                .dateOfBirth(LocalDateTime.of(1978, 12, 10, 0, 0))
                .build();
        customer3 = customerRepository.save(customer3);
        
        // Bob's business account
        Account bobBusiness = Account.builder()
                .accountType("BUSINESS")
                .balance(new BigDecimal("100000.00"))
                .availableBalance(new BigDecimal("100000.00"))
                .currency("USD")
                .minimumBalance(new BigDecimal("5000.00"))
                .customer(customer3)
                .build();
        
        accountRepository.save(bobBusiness);
        
        log.info("Customers and accounts seeded: 3 customers, 5 accounts created");
    }

    private void seedUsers() {
        log.info("Seeding users...");
        
        // Get customers and roles
        Customer customer1 = customerRepository.findByEmail("john.doe@example.com").orElse(null);
        Customer customer2 = customerRepository.findByEmail("jane.smith@example.com").orElse(null);
        Customer customer3 = customerRepository.findByEmail("bob.johnson@business.com").orElse(null);
        
        Role customerRole = roleRepository.findByName("CUSTOMER").orElse(null);
        Role accountantRole = roleRepository.findByName("ACCOUNTANT").orElse(null);
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        Role superadminRole = roleRepository.findByName("SUPERADMIN").orElse(null);
        
        // Customer users
        if (customer1 != null && customerRole != null) {
            User johnUser = User.builder()
                    .username("john_doe")
                    .email("john.doe@example.com")
                    .mobile("+1234567890")
                    .password(passwordEncoder.encode("password123"))
                    .customer(customer1)
                    .status("ACTIVE")
                    .build();
            johnUser.addRole(customerRole);
            userRepository.save(johnUser);
            log.info("Created user: john_doe (CUSTOMER)");
        }
        
        if (customer2 != null && customerRole != null) {
            User janeUser = User.builder()
                    .username("jane_smith")
                    .email("jane.smith@example.com")
                    .mobile("+1234567891")
                    .password(passwordEncoder.encode("password123"))
                    .customer(customer2)
                    .status("ACTIVE")
                    .build();
            janeUser.addRole(customerRole);
            userRepository.save(janeUser);
            log.info("Created user: jane_smith (CUSTOMER)");
        }
        
        if (customer3 != null && customerRole != null) {
            User bobUser = User.builder()
                    .username("bob_johnson")
                    .email("bob.johnson@business.com")
                    .mobile("+1234567892")
                    .password(passwordEncoder.encode("password123"))
                    .customer(customer3)
                    .status("ACTIVE")
                    .build();
            bobUser.addRole(customerRole);
            userRepository.save(bobUser);
            log.info("Created user: bob_johnson (CUSTOMER)");
        }
        
        // Staff users (without customer linkage)
        if (accountantRole != null) {
            User accountantUser = User.builder()
                    .username("accountant_mary")
                    .email("mary.accountant@nedbank.com")
                    .mobile("+1234567893")
                    .password(passwordEncoder.encode("accountant123"))
                    .status("ACTIVE")
                    .build();
            accountantUser.addRole(accountantRole);
            userRepository.save(accountantUser);
            log.info("Created user: accountant_mary (ACCOUNTANT)");
        }
        
        if (adminRole != null) {
            User adminUser = User.builder()
                    .username("admin_alex")
                    .email("alex.admin@nedbank.com")
                    .mobile("+1234567894")
                    .password(passwordEncoder.encode("admin123"))
                    .status("ACTIVE")
                    .build();
            adminUser.addRole(adminRole);
            userRepository.save(adminUser);
            log.info("Created user: admin_alex (ADMIN)");
        }
        
        if (superadminRole != null) {
            User superadminUser = User.builder()
                    .username("superadmin")
                    .email("superadmin@nedbank.com")
                    .mobile("+1234567895")
                    .password(passwordEncoder.encode("superadmin123"))
                    .status("ACTIVE")
                    .build();
            superadminUser.addRole(superadminRole);
            userRepository.save(superadminUser);
            log.info("Created user: superadmin (SUPERADMIN)");
        }
        
        log.info("Users seeded: 6 users created");
        
        // Log all created accounts for testing
        logSeedingSummary();
    }
    
    private void logSeedingSummary() {
        log.info("=== SEEDING SUMMARY ===");
        log.info("Permissions: {}", permissionRepository.count());
        log.info("Roles: {}", roleRepository.count());
        log.info("Customers: {}", customerRepository.count());
        log.info("Accounts: {}", accountRepository.count());
        log.info("Users: {}", userRepository.count());
        log.info("");
        log.info("Test Credentials:");
        log.info("   Customer: john_doe / password123");
        log.info("   Customer: jane_smith / password123");
        log.info("   Customer: bob_johnson / password123");
        log.info("   Accountant: accountant_mary / accountant123");
        log.info("   Admin: admin_alex / admin123");
        log.info("   Superadmin: superadmin / superadmin123");
        log.info("========================");
    }
    
    private void logExistingDataSummary() {
        log.info("=== EXISTING DATA SUMMARY ===");
        log.info("Permissions: {}", permissionRepository.count());
        log.info("Roles: {}", roleRepository.count());
        log.info("Customers: {}", customerRepository.count());
        log.info("Accounts: {}", accountRepository.count());
        log.info("Users: {}", userRepository.count());
        log.info("=============================");
    }
}
