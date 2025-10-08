package com.nedbank.banking.config;

import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.Customer;
import com.nedbank.banking.entity.Permission;
import com.nedbank.banking.entity.Role;
import com.nedbank.banking.entity.Transaction;
import com.nedbank.banking.entity.User;
import com.nedbank.banking.repository.AccountRepository;
import com.nedbank.banking.repository.CustomerRepository;
import com.nedbank.banking.repository.PermissionRepository;
import com.nedbank.banking.repository.RoleRepository;
import com.nedbank.banking.repository.TransactionRepository;
import com.nedbank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    @Transactional
    public void seedDatabase() {
        log.info("Starting database seeding...");

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
        seedTransactions();
        
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
        
        // Customer 1 - John Doe (ACTIVE - Fully approved customer with accounts)
        String johnOtherInfo = createOtherInfoJson(
            "123 Main Street", "New York", "NY", "10001",
            "Software Engineer", 95000.0,
            "Jane Doe", "+1234567899"
        );
        
        Customer customer1 = Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .mobile("+1234567890")
                .address("123 Main Street, New York, NY 10001")
                .nationalId("123456789")
                .dateOfBirth(LocalDateTime.of(1985, 5, 15, 0, 0))
                .otherInfo(johnOtherInfo)
                .status(Customer.STATUS_ACTIVE)
                .build();
        customer1 = customerRepository.save(customer1);
        
        // John's accounts
        Account johnSavings = Account.builder()
                .accountType("SAVINGS")
                .balance(new BigDecimal("15000.00"))
                .availableBalance(new BigDecimal("15000.00"))
                .currency("INR")
                .interestRate(new BigDecimal("2.5"))
                .minimumBalance(new BigDecimal("1000.00"))
                .customer(customer1)
                .build();
        
        Account johnCurrent = Account.builder()
                .accountType("CURRENT")
                .balance(new BigDecimal("5500.75"))
                .availableBalance(new BigDecimal("5500.75"))
                .currency("INR")
                .minimumBalance(new BigDecimal("500.00"))
                .customer(customer1)
                .build();
        
        accountRepository.saveAll(Arrays.asList(johnSavings, johnCurrent));
        
        // Customer 2 - Jane Smith (ACTIVE - Fully approved customer with accounts)
        String janeOtherInfo = createOtherInfoJson(
            "456 Oak Avenue", "Los Angeles", "CA", "90210",
            "Marketing Manager", 85000.0,
            "Michael Smith", "+1234567888"
        );
        
        Customer customer2 = Customer.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .mobile("+1234567891")
                .address("456 Oak Avenue, Los Angeles, CA 90210")
                .nationalId("987654321")
                .dateOfBirth(LocalDateTime.of(1990, 8, 22, 0, 0))
                .otherInfo(janeOtherInfo)
                .status(Customer.STATUS_ACTIVE)
                .build();
        customer2 = customerRepository.save(customer2);
        
        // Jane's accounts
        Account janeSavings = Account.builder()
                .accountType("SAVINGS")
                .balance(new BigDecimal("25000.00"))
                .availableBalance(new BigDecimal("25000.00"))
                .currency("INR")
                .interestRate(new BigDecimal("2.75"))
                .minimumBalance(new BigDecimal("1000.00"))
                .customer(customer2)
                .build();
        
        Account janeFixed = Account.builder()
                .accountType("FIXED_DEPOSIT")
                .balance(new BigDecimal("50000.00"))
                .availableBalance(new BigDecimal("50000.00"))
                .currency("INR")
                .interestRate(new BigDecimal("4.5"))
                .minimumBalance(new BigDecimal("10000.00"))
                .customer(customer2)
                .build();
        
        accountRepository.saveAll(Arrays.asList(janeSavings, janeFixed));
        
        // Customer 3 - Bob Johnson (PENDING_REVIEW - Just submitted customer details)
        String bobOtherInfo = createOtherInfoJson(
            "789 Business Park", "Chicago", "IL", "60601",
            "Business Consultant", 120000.0,
            "Alice Johnson", "+1234567777"
        );
        
        Customer customer3 = Customer.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@business.com")
                .mobile("+1234567892")
                .address("789 Business Park, Chicago, IL 60601")
                .nationalId("456789123")
                .dateOfBirth(LocalDateTime.of(1978, 12, 10, 0, 0))
                .otherInfo(bobOtherInfo)
                .status(Customer.STATUS_PENDING_REVIEW)
                .build();
        customer3 = customerRepository.save(customer3);
        
        // Note: Bob has no accounts yet - customers in PENDING_REVIEW status don't get accounts
        // until they're approved (moved to ACTIVE status)
        
        log.info("Customers and accounts seeded: 3 customers, 4 accounts created (2 ACTIVE customers with accounts, 1 PENDING_REVIEW without accounts)");
    }

    private void seedUsers() {
        log.info("Seeding users for different workflow states...");
        
        // Get customers and roles
        Customer customer1 = customerRepository.findByEmail("john.doe@example.com").orElse(null);
        Customer customer2 = customerRepository.findByEmail("jane.smith@example.com").orElse(null);
        Customer customer3 = customerRepository.findByEmail("bob.johnson@business.com").orElse(null);
        
        Role customerRole = roleRepository.findByName("CUSTOMER").orElse(null);
        Role accountantRole = roleRepository.findByName("ACCOUNTANT").orElse(null);
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        Role superadminRole = roleRepository.findByName("SUPERADMIN").orElse(null);
        
        // === ACTIVE USERS (Fully approved with customer data and accounts) ===
        if (customer1 != null && customerRole != null) {
            User johnUser = User.builder()
                    .username("john_doe")
                    .email("john.doe@example.com")
                    .mobile("+1234567890")
                    .password(passwordEncoder.encode("password123"))
                    .customer(customer1)
                    .status(User.STATUS_ACTIVE)
                    .build();
            johnUser.addRole(customerRole);
            userRepository.save(johnUser);
            log.info("Created user: john_doe (CUSTOMER - ACTIVE with accounts)");
        }
        
        if (customer2 != null && customerRole != null) {
            User janeUser = User.builder()
                    .username("jane_smith")
                    .email("jane.smith@example.com")
                    .mobile("+1234567891")
                    .password(passwordEncoder.encode("password123"))
                    .customer(customer2)
                    .status(User.STATUS_ACTIVE)
                    .build();
            janeUser.addRole(customerRole);
            userRepository.save(janeUser);
            log.info("Created user: jane_smith (CUSTOMER - ACTIVE with accounts)");
        }
        
        // === PENDING_REVIEW USER (Submitted customer details, waiting for admin review) ===
        if (customer3 != null && customerRole != null) {
            User bobUser = User.builder()
                    .username("bob_johnson")
                    .email("bob.johnson@business.com")
                    .mobile("+1234567892")
                    .password(passwordEncoder.encode("password123"))
                    .customer(customer3)
                    .status(User.STATUS_PENDING_REVIEW)
                    .build();
            bobUser.addRole(customerRole);
            userRepository.save(bobUser);
            log.info("Created user: bob_johnson (CUSTOMER - PENDING_REVIEW, submitted customer details)");
        }
        
        // === PENDING_DETAILS USERS (Just registered, need to fill customer details) ===
        if (customerRole != null) {
            User pendingUser1 = User.builder()
                    .username("new_customer")
                    .email("new.customer@example.com")
                    .mobile("+1234567896")
                    .password(passwordEncoder.encode("password123"))
                    .status(User.STATUS_PENDING_DETAILS)
                    // No customer linked yet - they need to fill the form
                    .build();
            pendingUser1.addRole(customerRole);
            userRepository.save(pendingUser1);
            log.info("Created user: new_customer (CUSTOMER - PENDING_DETAILS, needs to fill customer details)");
            
            User pendingUser2 = User.builder()
                    .username("test_user")
                    .email("test.user@example.com")
                    .mobile("+1234567897")
                    .password(passwordEncoder.encode("password123"))
                    .status(User.STATUS_PENDING_DETAILS)
                    // No customer linked yet
                    .build();
            pendingUser2.addRole(customerRole);
            userRepository.save(pendingUser2);
            log.info("Created user: test_user (CUSTOMER - PENDING_DETAILS, needs to fill customer details)");
        }
        
        // === STAFF USERS (No customer data needed) ===
        if (accountantRole != null) {
            User accountantUser = User.builder()
                    .username("accountant_mary")
                    .email("mary.accountant@nedbank.com")
                    .mobile("+1234567893")
                    .password(passwordEncoder.encode("accountant123"))
                    .status(User.STATUS_ACTIVE)
                    .build();
            accountantUser.addRole(accountantRole);
            userRepository.save(accountantUser);
            log.info("Created user: accountant_mary (ACCOUNTANT - ACTIVE staff)");
        }
        
        if (adminRole != null) {
            User adminUser = User.builder()
                    .username("admin_alex")
                    .email("alex.admin@nedbank.com")
                    .mobile("+1234567894")
                    .password(passwordEncoder.encode("admin123"))
                    .status(User.STATUS_ACTIVE)
                    .build();
            adminUser.addRole(adminRole);
            userRepository.save(adminUser);
            log.info("Created user: admin_alex (ADMIN - ACTIVE staff)");
        }
        
        if (superadminRole != null) {
            User superadminUser = User.builder()
                    .username("superadmin")
                    .email("superadmin@nedbank.com")
                    .mobile("+1234567895")
                    .password(passwordEncoder.encode("superadmin123"))
                    .status(User.STATUS_ACTIVE)
                    .build();
            superadminUser.addRole(superadminRole);
            userRepository.save(superadminUser);
            log.info("Created user: superadmin (SUPERADMIN - ACTIVE staff)");
        }
        
        log.info("Users seeded: 8 users created (2 ACTIVE customers, 1 PENDING_REVIEW customer, 2 PENDING_DETAILS users, 3 staff)");
        
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
        log.info("Test Credentials by Workflow State:");
        log.info("");
        log.info("=== ACTIVE CUSTOMERS (Full banking access) ===");
        log.info("   john_doe / password123        - ACTIVE customer with 2 accounts");
        log.info("   jane_smith / password123      - ACTIVE customer with 2 accounts");
        log.info("");
        log.info("=== PENDING CUSTOMERS (Limited access) ===");
        log.info("   bob_johnson / password123     - PENDING_REVIEW, awaiting admin approval");
        log.info("   new_customer / password123    - PENDING_DETAILS, needs to fill customer form");
        log.info("   test_user / password123       - PENDING_DETAILS, needs to fill customer form");
        log.info("");
        log.info("=== STAFF USERS (Admin access) ===");
        log.info("   accountant_mary / accountant123  - ACCOUNTANT role");
        log.info("   admin_alex / admin123           - ADMIN role (can manage users)");
        log.info("   superadmin / superadmin123      - SUPERADMIN role (full access)");
        log.info("========================");
    }
    
    private void seedTransactions() {
        log.info("Seeding transactions...");
        
        // Get accounts for transactions
        List<Account> accounts = accountRepository.findAll();
        if (accounts.isEmpty()) {
            log.warn("No accounts found. Skipping transaction seeding.");
            return;
        }
        
        // Find specific accounts for testing
        Account johnSavingsAccount = accounts.stream()
                .filter(acc -> acc.getAccountType().equals("SAVINGS") && 
                              acc.getCustomer().getFirstName().equals("John"))
                .findFirst().orElse(accounts.get(0));
                
        Account johnCheckingAccount = accounts.stream()
                .filter(acc -> acc.getAccountType().equals("CURRENT") && 
                              acc.getCustomer().getFirstName().equals("John"))
                .findFirst().orElse(accounts.get(0));
                
        Account janeAccount = accounts.stream()
                .filter(acc -> acc.getCustomer().getFirstName().equals("Jane"))
                .findFirst().orElse(null);
        
        // Log which accounts will have transactions
        log.info("Transaction seeding targets:");
        log.info("  - John SAVINGS: {} (ID: {})", johnSavingsAccount.getAccountNumber(), johnSavingsAccount.getId());
        log.info("  - John CURRENT: {} (ID: {})", johnCheckingAccount.getAccountNumber(), johnCheckingAccount.getId());
        if (janeAccount != null) {
            log.info("  - Jane account: {} (ID: {})", janeAccount.getAccountNumber(), janeAccount.getId());
        }
        
        // Use a base date in the past for consistent transaction dates
        LocalDateTime baseDate = LocalDateTime.now().minusDays(15); // Center transactions around 15 days ago
        log.info("Transaction base date: {} (transactions will span 30 days before this)", baseDate.toLocalDate());
        
        int transactionCount = 0;
        
        // === CUSTOMER TRANSACTIONS (ONLY DEBIT AND CREDIT) ===
        
        // 1. Credit - Salary - 30 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_CREDIT, BigDecimal.valueOf(5000.00),
            "Monthly salary deposit", Transaction.CATEGORY_SALARY,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(30)
        );
        
        // 2. Debit - Rent Payment - 29 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_DEBIT, BigDecimal.valueOf(1200.00),
            "Rent payment", Transaction.CATEGORY_PAYMENT,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(29)
        );
        
        // 3. Debit - Utility Bill - 28 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_DEBIT, BigDecimal.valueOf(150.00),
            "Electricity bill", Transaction.CATEGORY_BILL_PAYMENT,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(28)
        );
        
        // 4. Credit - Refund - 25 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_CREDIT, BigDecimal.valueOf(75.00),
            "Refund for returned item", Transaction.CATEGORY_REFUND,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(25)
        );
        
        // 5. Debit - Grocery Shopping - 24 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_DEBIT, BigDecimal.valueOf(250.00),
            "Grocery shopping", Transaction.CATEGORY_PAYMENT,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(24)
        );
        
        // 6. Debit - Restaurant - 20 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_DEBIT, BigDecimal.valueOf(85.00),
            "Restaurant payment", Transaction.CATEGORY_PAYMENT,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(20)
        );
        
        // 7. Debit - Online Shopping - 18 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_DEBIT, BigDecimal.valueOf(180.00),
            "Online purchase", Transaction.CATEGORY_PAYMENT,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(18)
        );
        
        // 8. Credit - Bonus - 15 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_CREDIT, BigDecimal.valueOf(500.00),
            "Performance bonus", Transaction.CATEGORY_SALARY,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(15)
        );
        
        // 9. Debit - Gas Station - 10 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_DEBIT, BigDecimal.valueOf(60.00),
            "Gas station payment", Transaction.CATEGORY_PAYMENT,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(10)
        );
        
        // 10. Debit - Pharmacy - 7 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_DEBIT, BigDecimal.valueOf(42.00),
            "Pharmacy purchase", Transaction.CATEGORY_PAYMENT,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(7)
        );
        
        // 11. Credit - Payment received - 5 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_CREDIT, BigDecimal.valueOf(120.00),
            "Payment received", Transaction.CATEGORY_OTHER,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(5)
        );
        
        // 12. Debit - Entertainment - 3 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_DEBIT, BigDecimal.valueOf(95.00),
            "Movie tickets and dinner", Transaction.CATEGORY_PAYMENT,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(3)
        );
        
        // 13. Credit - Cashback - 2 days ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_CREDIT, BigDecimal.valueOf(25.00),
            "Cashback reward", Transaction.CATEGORY_REFUND,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(2)
        );
        
        // 14. Debit - Coffee shop - 1 day ago
        transactionCount += createTransaction(
            johnCheckingAccount, null,
            Transaction.TYPE_DEBIT, BigDecimal.valueOf(15.50),
            "Coffee and snacks", Transaction.CATEGORY_PAYMENT,
            Transaction.STATUS_COMPLETED, "john_doe", baseDate.minusDays(1)
        );
        
        // === JANE'S TRANSACTIONS (ONLY DEBIT AND CREDIT) ===
        if (janeAccount != null) {
            // Credit - Salary
            transactionCount += createTransaction(
                janeAccount, null,
                Transaction.TYPE_CREDIT, BigDecimal.valueOf(4500.00),
                "Monthly salary", Transaction.CATEGORY_SALARY,
                Transaction.STATUS_COMPLETED, "jane_smith", baseDate.minusDays(30)
            );
            
            // Debit - Rent
            transactionCount += createTransaction(
                janeAccount, null,
                Transaction.TYPE_DEBIT, BigDecimal.valueOf(1500.00),
                "Rent payment", Transaction.CATEGORY_PAYMENT,
                Transaction.STATUS_COMPLETED, "jane_smith", baseDate.minusDays(28)
            );
            
            // Debit - Utilities
            transactionCount += createTransaction(
                janeAccount, null,
                Transaction.TYPE_DEBIT, BigDecimal.valueOf(200.00),
                "Utility bills", Transaction.CATEGORY_BILL_PAYMENT,
                Transaction.STATUS_COMPLETED, "jane_smith", baseDate.minusDays(25)
            );
            
            // Debit - Shopping
            transactionCount += createTransaction(
                janeAccount, null,
                Transaction.TYPE_DEBIT, BigDecimal.valueOf(300.00),
                "Shopping", Transaction.CATEGORY_PAYMENT,
                Transaction.STATUS_COMPLETED, "jane_smith", baseDate.minusDays(15)
            );
            
            // Credit - Refund
            transactionCount += createTransaction(
                janeAccount, null,
                Transaction.TYPE_CREDIT, BigDecimal.valueOf(50.00),
                "Product refund", Transaction.CATEGORY_REFUND,
                Transaction.STATUS_COMPLETED, "jane_smith", baseDate.minusDays(10)
            );
        }
        
        // === ACCOUNTANT BULK UPLOAD BATCH ===
        // Bulk transactions created by accountant on Jane's account (separate from customer transactions)
        String batchId = "BATCH12345678";
        
        if (janeAccount != null) {
            for (int i = 1; i <= 10; i++) {
                Transaction bulkTxn = createBulkTransaction(
                    janeAccount,
                    i % 2 == 0 ? Transaction.TYPE_CREDIT : Transaction.TYPE_DEBIT,
                    BigDecimal.valueOf(100.00 + (i * 25)),
                    "Bulk transaction #" + i + " - Batch processing",
                    batchId,
                    "accountant_mary",
                    baseDate.minusDays(12)
                );
                
                if (bulkTxn != null) {
                    transactionCount++;
                }
            }
        }
        
        log.info("Seeded {} transactions successfully", transactionCount);
        log.info("  - John's customer transactions: 14 (DEBIT/CREDIT only)");
        log.info("  - Jane's customer transactions: 5 (DEBIT/CREDIT only)");
        log.info("  - Jane's accountant bulk transactions: 10 (processed by accountant_mary)");
        log.info("Transaction date range: {} to {}", 
                baseDate.minusDays(30).toLocalDate(), 
                baseDate.toLocalDate());
    }
    
    private int createTransaction(Account account, Account destinationAccount, 
                                  String type, BigDecimal amount, String description, 
                                  String category, String status, String initiatedBy,
                                  LocalDateTime transactionDate) {
        return createTransaction(account, destinationAccount, type, amount, description, 
                                category, status, initiatedBy, transactionDate, null);
    }
    
    private int createTransaction(Account account, Account destinationAccount, 
                                  String type, BigDecimal amount, String description, 
                                  String category, String status, String initiatedBy,
                                  LocalDateTime transactionDate, String failureReason) {
        try {
            BigDecimal balanceBefore = account.getBalance();
            BigDecimal balanceAfter = balanceBefore;
            
            // Update balance if completed
            if (Transaction.STATUS_COMPLETED.equals(status)) {
                if (type.equals(Transaction.TYPE_DEBIT) || 
                    type.equals(Transaction.TYPE_WITHDRAWAL) || 
                    type.equals(Transaction.TYPE_FEE) ||
                    type.equals(Transaction.TYPE_TRANSFER)) {
                    balanceAfter = balanceBefore.subtract(amount);
                    account.setBalance(balanceAfter);
                    account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
                } else {
                    balanceAfter = balanceBefore.add(amount);
                    account.setBalance(balanceAfter);
                    account.setAvailableBalance(account.getAvailableBalance().add(amount));
                }
                account.setLastTransactionDate(transactionDate);
                accountRepository.save(account);
            }
            
            Transaction transaction = Transaction.builder()
                    .account(account)
                    .destinationAccount(destinationAccount)
                    .transactionType(type)
                    .amount(amount)
                    .currency("INR")
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .description(description)
                    .category(category)
                    .status(status)
                    .transactionDate(transactionDate)
                    .valueDate(transactionDate)
                    .initiatedBy(initiatedBy)
                    .approvedBy(Transaction.STATUS_COMPLETED.equals(status) ? initiatedBy : null)
                    .approvalDate(Transaction.STATUS_COMPLETED.equals(status) ? transactionDate : null)
                    .failureReason(failureReason)
                    .isBulkUpload(false)
                    .build();
            
            // Manually set created/updated dates
            transaction.setCreatedAt(transactionDate);
            transaction.setUpdatedAt(transactionDate);
            
            transactionRepository.save(transaction);
            return 1;
        } catch (Exception e) {
            log.warn("Failed to create transaction: {}", e.getMessage());
            return 0;
        }
    }
    
    private int createTransfer(Account sourceAccount, Account destinationAccount, 
                              BigDecimal amount, String description, String status, 
                              String initiatedBy, LocalDateTime transactionDate) {
        try {
            // Source account transaction (debit)
            BigDecimal sourceBalanceBefore = sourceAccount.getBalance();
            BigDecimal sourceBalanceAfter = sourceBalanceBefore.subtract(amount);
            
            sourceAccount.setBalance(sourceBalanceAfter);
            sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(amount));
            sourceAccount.setLastTransactionDate(transactionDate);
            accountRepository.save(sourceAccount);
            
            // Destination account balance update
            BigDecimal destBalanceBefore = destinationAccount.getBalance();
            BigDecimal destBalanceAfter = destBalanceBefore.add(amount);
            
            destinationAccount.setBalance(destBalanceAfter);
            destinationAccount.setAvailableBalance(destinationAccount.getAvailableBalance().add(amount));
            destinationAccount.setLastTransactionDate(transactionDate);
            accountRepository.save(destinationAccount);
            
            // Create transfer transaction
            Transaction transaction = Transaction.builder()
                    .account(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .transactionType(Transaction.TYPE_TRANSFER)
                    .amount(amount)
                    .currency("INR")
                    .balanceBefore(sourceBalanceBefore)
                    .balanceAfter(sourceBalanceAfter)
                    .description(description)
                    .category(Transaction.CATEGORY_TRANSFER)
                    .status(status)
                    .transactionDate(transactionDate)
                    .valueDate(transactionDate)
                    .initiatedBy(initiatedBy)
                    .approvedBy(status.equals(Transaction.STATUS_COMPLETED) ? initiatedBy : null)
                    .approvalDate(status.equals(Transaction.STATUS_COMPLETED) ? transactionDate : null)
                    .isBulkUpload(false)
                    .build();
            
            transaction.setCreatedAt(transactionDate);
            transaction.setUpdatedAt(transactionDate);
            
            transactionRepository.save(transaction);
            return 1;
        } catch (Exception e) {
            log.warn("Failed to create transfer transaction: {}", e.getMessage());
            return 0;
        }
    }
    
    private Transaction createBulkTransaction(Account account, String type, BigDecimal amount, 
                                             String description, String batchId, 
                                             String initiatedBy, LocalDateTime transactionDate) {
        try {
            BigDecimal balanceBefore = account.getBalance();
            BigDecimal balanceAfter = balanceBefore;
            
            // Update balance
            if (type.equals(Transaction.TYPE_DEBIT)) {
                balanceAfter = balanceBefore.subtract(amount);
            } else {
                balanceAfter = balanceBefore.add(amount);
            }
            
            account.setBalance(balanceAfter);
            account.setAvailableBalance(balanceAfter);
            account.setLastTransactionDate(transactionDate);
            accountRepository.save(account);
            
            Transaction transaction = Transaction.builder()
                    .account(account)
                    .transactionType(type)
                    .amount(amount)
                    .currency("INR")
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .description(description)
                    .category(Transaction.CATEGORY_OTHER)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(transactionDate)
                    .valueDate(transactionDate)
                    .initiatedBy(initiatedBy)
                    .approvedBy(initiatedBy)
                    .approvalDate(transactionDate)
                    .isBulkUpload(true)
                    .bulkUploadBatchId(batchId)
                    .build();
            
            transaction.setCreatedAt(transactionDate);
            transaction.setUpdatedAt(transactionDate);
            
            return transactionRepository.save(transaction);
        } catch (Exception e) {
            log.warn("Failed to create bulk transaction: {}", e.getMessage());
            return null;
        }
    }
    
    private void logExistingDataSummary() {
        log.info("=== EXISTING DATA SUMMARY ===");
        log.info("Permissions: {}", permissionRepository.count());
        log.info("Roles: {}", roleRepository.count());
        log.info("Customers: {}", customerRepository.count());
        log.info("Accounts: {}", accountRepository.count());
        log.info("Users: {}", userRepository.count());
        log.info("Transactions: {}", transactionRepository.count());
        log.info("=============================");
    }
    
    /**
     * Helper method to create otherInfo JSON string for customer data
     */
    private String createOtherInfoJson(String streetAddress, String city, String state, String zipCode,
                                     String occupation, Double annualIncome, 
                                     String emergencyContactName, String emergencyContactPhone) {
        try {
            var otherInfoMap = new java.util.HashMap<String, Object>();
            
            // Address components
            otherInfoMap.put("streetAddress", streetAddress);
            otherInfoMap.put("city", city);
            otherInfoMap.put("state", state);
            otherInfoMap.put("zipCode", zipCode);
            
            // Personal information
            otherInfoMap.put("occupation", occupation);
            otherInfoMap.put("annualIncome", annualIncome);
            
            // Emergency contact
            otherInfoMap.put("emergencyContactName", emergencyContactName);
            otherInfoMap.put("emergencyContactPhone", emergencyContactPhone);
            
            // Additional sample data
            otherInfoMap.put("maritalStatus", "Single");
            otherInfoMap.put("employmentType", "Full-time");
            otherInfoMap.put("yearsAtCurrentJob", 3);
            otherInfoMap.put("hasOtherBankAccounts", false);
            otherInfoMap.put("preferredContactMethod", "email");
            
            return objectMapper.writeValueAsString(otherInfoMap);
        } catch (Exception e) {
            log.warn("Failed to create otherInfo JSON: {}", e.getMessage());
            return null;
        }
    }
}
