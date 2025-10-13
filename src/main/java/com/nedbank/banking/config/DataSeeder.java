package com.nedbank.banking.config;

import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.Beneficiary;
import com.nedbank.banking.entity.Customer;
import com.nedbank.banking.entity.EFTTransaction;
import com.nedbank.banking.entity.Permission;
import com.nedbank.banking.entity.Role;
import com.nedbank.banking.entity.Transaction;
import com.nedbank.banking.entity.User;
import com.nedbank.banking.repository.AccountRepository;
import com.nedbank.banking.repository.BeneficiaryRepository;
import com.nedbank.banking.repository.CustomerRepository;
import com.nedbank.banking.repository.EFTTransactionRepository;
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
    private final BeneficiaryRepository beneficiaryRepository;
    private final EFTTransactionRepository eftTransactionRepository;
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
        seedBeneficiaries();
        seedEFTTransactions();
        
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
    
    /**
     * Seed beneficiaries for NEFT transfers
     */
    private void seedBeneficiaries() {
        log.info("Seeding beneficiaries...");
        
        try {
            // Get customers
            Customer customer1 = customerRepository.findByEmail("john.doe@example.com").orElse(null);
            Customer customer2 = customerRepository.findByEmail("jane.smith@example.com").orElse(null);
            Customer customer3 = customerRepository.findByEmail("bob.johnson@business.com").orElse(null);
            
            if (customer1 == null || customer2 == null) {
                log.warn("Some customers not found, skipping beneficiary seeding");
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            
            // Customer 1 Beneficiaries
            if (customer1 != null) {
                createBeneficiary(customer1, "Rajesh Kumar", "123456789012", "HDFC0001234", 
                    "HDFC Bank", "Mumbai Main Branch", "Rajesh", "9876543210", 
                    "rajesh@example.com", "john.doe@example.com", now.minusDays(30));
                
                createBeneficiary(customer1, "Priya Sharma", "234567890123", "ICIC0005678", 
                    "ICICI Bank", "Delhi Branch", "Priya", "9876543211", 
                    "priya@example.com", "john.doe@example.com", now.minusDays(25));
                
                createBeneficiary(customer1, "Amit Patel", "345678901234", "SBIN0002345", 
                    "State Bank of India", "Bangalore Branch", "Amit", "9876543212", 
                    "amit@example.com", "john.doe@example.com", now.minusDays(20));
                
                createBeneficiary(customer1, "Neha Gupta", "456789012345", "AXIS0003456", 
                    "Axis Bank", "Chennai Branch", "Neha", "9876543213", 
                    "neha@example.com", "john.doe@example.com", now.minusDays(15));
            }
            
            // Customer 2 Beneficiaries
            if (customer2 != null) {
                createBeneficiary(customer2, "Suresh Reddy", "567890123456", "PUNB0004567", 
                    "Punjab National Bank", "Hyderabad Branch", "Suresh", "9876543214", 
                    "suresh@example.com", "jane.smith@example.com", now.minusDays(28));
                
                createBeneficiary(customer2, "Lakshmi Iyer", "678901234567", "KKBK0005678", 
                    "Kotak Mahindra Bank", "Pune Branch", "Lakshmi", "9876543215", 
                    "lakshmi@example.com", "jane.smith@example.com", now.minusDays(22));
                
                createBeneficiary(customer2, "Vikram Singh", "789012345678", "YESB0006789", 
                    "YES Bank", "Kolkata Branch", "Vikram", "9876543216", 
                    "vikram@example.com", "jane.smith@example.com", now.minusDays(18));
            }
            
            // Customer 3 Beneficiaries (if available)
            if (customer3 != null) {
                createBeneficiary(customer3, "Anjali Mehta", "890123456789", "IDFB0007890", 
                    "IDFC First Bank", "Ahmedabad Branch", "Anjali", "9876543217", 
                    "anjali@example.com", "bob.johnson@business.com", now.minusDays(26));
                
                createBeneficiary(customer3, "Karan Malhotra", "901234567890", "BARB0008901", 
                    "Bank of Baroda", "Jaipur Branch", "Karan", "9876543218", 
                    "karan@example.com", "bob.johnson@business.com", now.minusDays(14));
                
                createBeneficiary(customer3, "Deepa Nair", "012345678901", "CNRB0009012", 
                    "Canara Bank", "Cochin Branch", "Deepa", "9876543219", 
                    "deepa@example.com", "bob.johnson@business.com", now.minusDays(10));
            }
            
            log.info("Successfully seeded {} beneficiaries", beneficiaryRepository.count());
            
        } catch (Exception e) {
            log.error("Error seeding beneficiaries: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a single beneficiary
     */
    private void createBeneficiary(Customer customer, String beneficiaryName, String accountNumber,
                                   String ifscCode, String bankName, String branchName, String nickname,
                                   String mobile, String email, String addedBy, LocalDateTime createdAt) {
        try {
            Beneficiary beneficiary = Beneficiary.builder()
                    .customer(customer)
                    .beneficiaryName(beneficiaryName)
                    .accountNumber(accountNumber)
                    .ifscCode(ifscCode)
                    .bankName(bankName)
                    .branchName(branchName)
                    .nickname(nickname)
                    .mobile(mobile)
                    .email(email)
                    .isVerified(true) // Mark as verified for seed data
                    .status(Beneficiary.STATUS_ACTIVE)
                    .addedBy(addedBy)
                    .verifiedBy("system")
                    .verifiedAt(createdAt.plusDays(1))
                    .lastUsedAt(createdAt.plusDays(5))
                    .build();
            
            beneficiary.setCreatedAt(createdAt);
            beneficiary.setUpdatedAt(createdAt);
            
            beneficiaryRepository.save(beneficiary);
            log.debug("Created beneficiary: {} for customer: {}", beneficiaryName, customer.getEmail());
            
        } catch (Exception e) {
            log.warn("Failed to create beneficiary {}: {}", beneficiaryName, e.getMessage());
        }
    }
    
    /**
     * Seed EFT transactions (NEFT, RTGS, IMPS)
     */
    private void seedEFTTransactions() {
        log.info("Seeding EFT transactions...");
        
        try {
            // Get customers and accounts
            Customer customer1 = customerRepository.findByEmail("john.doe@example.com").orElse(null);
            Customer customer2 = customerRepository.findByEmail("jane.smith@example.com").orElse(null);
            Customer customer3 = customerRepository.findByEmail("bob.johnson@business.com").orElse(null);
            
            if (customer1 == null || customer2 == null) {
                log.warn("Some customers not found, skipping EFT transaction seeding");
                return;
            }
            
            List<Account> customer1Accounts = accountRepository.findByCustomerId(customer1.getId());
            List<Account> customer2Accounts = accountRepository.findByCustomerId(customer2.getId());
            
            if (customer1Accounts.isEmpty() || customer2Accounts.isEmpty()) {
                log.warn("Some accounts not found, skipping EFT transaction seeding");
                return;
            }
            
            Account account1 = customer1Accounts.get(0);
            Account account2 = customer2Accounts.get(0);
            
            // Get beneficiaries
            List<Beneficiary> customer1Beneficiaries = beneficiaryRepository.findByCustomerIdOrderByCreatedAtDesc(customer1.getId());
            List<Beneficiary> customer2Beneficiaries = beneficiaryRepository.findByCustomerIdOrderByCreatedAtDesc(customer2.getId());
            
            LocalDateTime now = LocalDateTime.now();
            
            // Create completed NEFT transactions (past)
            if (!customer1Beneficiaries.isEmpty()) {
                createCompletedNEFT(account1, customer1Beneficiaries.get(0), 
                    new BigDecimal("50000.00"), new BigDecimal("5.00"),
                    "Invoice Payment", "INV-2024-001", "john.doe@example.com",
                    now.minusDays(5), "NEFT20251003010");
                
                if (customer1Beneficiaries.size() > 1) {
                    createCompletedNEFT(account1, customer1Beneficiaries.get(1), 
                        new BigDecimal("25000.00"), new BigDecimal("5.00"),
                        "Vendor Payment", "PO-5678", "john.doe@example.com",
                        now.minusDays(3), "NEFT20251005011");
                }
            }
            
            if (!customer2Beneficiaries.isEmpty()) {
                createCompletedNEFT(account2, customer2Beneficiaries.get(0), 
                    new BigDecimal("75000.00"), new BigDecimal("15.00"),
                    "Service Payment", "SRV-9012", "jane.smith@example.com",
                    now.minusDays(4), "NEFT20251004010");
                
                if (customer2Beneficiaries.size() > 1) {
                    createCompletedNEFT(account2, customer2Beneficiaries.get(1), 
                        new BigDecimal("15000.00"), new BigDecimal("5.00"),
                        "Consulting Fee", "CONS-3456", "jane.smith@example.com",
                        now.minusDays(2), "NEFT20251006014");
                }
            }
            
            // Create pending NEFT transactions (for testing)
            if (!customer1Beneficiaries.isEmpty() && customer1Beneficiaries.size() > 2) {
                createPendingNEFT(account1, customer1Beneficiaries.get(2), 
                    new BigDecimal("30000.00"), new BigDecimal("5.00"),
                    "Pending Payment", "Testing pending status", "john.doe@example.com",
                    now.minusHours(2));
            }
            
            // Create failed NEFT transaction (for testing refund scenario)
            if (!customer2Beneficiaries.isEmpty() && customer2Beneficiaries.size() > 2) {
                createFailedNEFT(account2, customer2Beneficiaries.get(2), 
                    new BigDecimal("10000.00"), new BigDecimal("2.50"),
                    "Failed Payment", "Testing failure", "jane.smith@example.com",
                    now.minusDays(1), "NEFT20251007015",
                    "Simulated: Beneficiary account not found");
            }
            
            log.info("Successfully seeded {} EFT transactions", eftTransactionRepository.count());
            
        } catch (Exception e) {
            log.error("Error seeding EFT transactions: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a completed NEFT transaction
     */
    private void createCompletedNEFT(Account sourceAccount, Beneficiary beneficiary,
                                     BigDecimal amount, BigDecimal charges,
                                     String purpose, String remarks, String initiatedBy,
                                     LocalDateTime transactionDate, String batchId) {
        try {
            BigDecimal totalAmount = amount.add(charges);
            
            // Create the internal transaction (debit)
            Transaction transaction = Transaction.builder()
                    .account(sourceAccount)
                    .transactionType(Transaction.TYPE_DEBIT)
                    .amount(totalAmount)
                    .currency("INR")
                    .balanceBefore(sourceAccount.getBalance().add(totalAmount))
                    .balanceAfter(sourceAccount.getBalance())
                    .description("NEFT Transfer to " + beneficiary.getBeneficiaryName() + " - " + purpose)
                    .category(Transaction.CATEGORY_TRANSFER)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(transactionDate)
                    .valueDate(transactionDate)
                    .initiatedBy(initiatedBy)
                    .approvedBy(initiatedBy)
                    .approvalDate(transactionDate)
                    .build();
            
            transaction.setCreatedAt(transactionDate);
            transaction.setUpdatedAt(transactionDate);
            Transaction savedTransaction = transactionRepository.save(transaction);
            
            // Create the EFT transaction
            EFTTransaction eftTransaction = EFTTransaction.builder()
                    .eftType(EFTTransaction.TYPE_NEFT)
                    .sourceAccount(sourceAccount)
                    .beneficiary(beneficiary)
                    .beneficiaryAccountNumber(beneficiary.getAccountNumber())
                    .beneficiaryName(beneficiary.getBeneficiaryName())
                    .beneficiaryIfsc(beneficiary.getIfscCode())
                    .beneficiaryBankName(beneficiary.getBankName())
                    .amount(amount)
                    .charges(charges)
                    .totalAmount(totalAmount)
                    .currency("INR")
                    .purpose(purpose)
                    .remarks(remarks)
                    .status(EFTTransaction.STATUS_COMPLETED)
                    .batchId(batchId)
                    .batchTime(transactionDate.toLocalTime().withMinute(0).withSecond(0))
                    .estimatedCompletion(transactionDate.plusMinutes(30))
                    .actualCompletion(transactionDate.plusMinutes(15))
                    .initiatedBy(initiatedBy)
                    .processedBy("NEFT_BATCH_PROCESSOR")
                    .transaction(savedTransaction)
                    .build();
            
            eftTransaction.setCreatedAt(transactionDate);
            eftTransaction.setUpdatedAt(transactionDate.plusMinutes(15));
            eftTransactionRepository.save(eftTransaction);
            
            log.debug("Created completed NEFT transaction: {} for amount {}", 
                eftTransaction.getEftReference(), amount);
            
        } catch (Exception e) {
            log.warn("Failed to create completed NEFT transaction: {}", e.getMessage());
        }
    }
    
    /**
     * Create a pending NEFT transaction (for testing)
     */
    private void createPendingNEFT(Account sourceAccount, Beneficiary beneficiary,
                                   BigDecimal amount, BigDecimal charges,
                                   String purpose, String remarks, String initiatedBy,
                                   LocalDateTime transactionDate) {
        try {
            BigDecimal totalAmount = amount.add(charges);
            
            // Create the internal transaction (debit)
            Transaction transaction = Transaction.builder()
                    .account(sourceAccount)
                    .transactionType(Transaction.TYPE_DEBIT)
                    .amount(totalAmount)
                    .currency("INR")
                    .balanceBefore(sourceAccount.getBalance().add(totalAmount))
                    .balanceAfter(sourceAccount.getBalance())
                    .description("NEFT Transfer to " + beneficiary.getBeneficiaryName() + " - " + purpose)
                    .category(Transaction.CATEGORY_TRANSFER)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(transactionDate)
                    .valueDate(transactionDate)
                    .initiatedBy(initiatedBy)
                    .approvedBy(initiatedBy)
                    .approvalDate(transactionDate)
                    .build();
            
            transaction.setCreatedAt(transactionDate);
            transaction.setUpdatedAt(transactionDate);
            Transaction savedTransaction = transactionRepository.save(transaction);
            
            // Calculate next batch time
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime nextBatchTime = now.plusHours(1).withMinute(0).withSecond(0);
            
            // Create the EFT transaction
            EFTTransaction eftTransaction = EFTTransaction.builder()
                    .eftType(EFTTransaction.TYPE_NEFT)
                    .sourceAccount(sourceAccount)
                    .beneficiary(beneficiary)
                    .beneficiaryAccountNumber(beneficiary.getAccountNumber())
                    .beneficiaryName(beneficiary.getBeneficiaryName())
                    .beneficiaryIfsc(beneficiary.getIfscCode())
                    .beneficiaryBankName(beneficiary.getBankName())
                    .amount(amount)
                    .charges(charges)
                    .totalAmount(totalAmount)
                    .currency("INR")
                    .purpose(purpose)
                    .remarks(remarks)
                    .status(EFTTransaction.STATUS_PENDING)
                    .batchTime(nextBatchTime)
                    .estimatedCompletion(LocalDateTime.now().plusHours(1).plusMinutes(30))
                    .initiatedBy(initiatedBy)
                    .transaction(savedTransaction)
                    .build();
            
            eftTransaction.setCreatedAt(transactionDate);
            eftTransaction.setUpdatedAt(transactionDate);
            eftTransactionRepository.save(eftTransaction);
            
            log.debug("Created pending NEFT transaction: {} for amount {}", 
                eftTransaction.getEftReference(), amount);
            
        } catch (Exception e) {
            log.warn("Failed to create pending NEFT transaction: {}", e.getMessage());
        }
    }
    
    /**
     * Create a failed NEFT transaction (for testing refund scenario)
     */
    private void createFailedNEFT(Account sourceAccount, Beneficiary beneficiary,
                                  BigDecimal amount, BigDecimal charges,
                                  String purpose, String remarks, String initiatedBy,
                                  LocalDateTime transactionDate, String batchId,
                                  String failureReason) {
        try {
            BigDecimal totalAmount = amount.add(charges);
            
            // Create the initial debit transaction
            Transaction debitTransaction = Transaction.builder()
                    .account(sourceAccount)
                    .transactionType(Transaction.TYPE_DEBIT)
                    .amount(totalAmount)
                    .currency("INR")
                    .balanceBefore(sourceAccount.getBalance().add(totalAmount))
                    .balanceAfter(sourceAccount.getBalance())
                    .description("NEFT Transfer to " + beneficiary.getBeneficiaryName() + " - " + purpose)
                    .category(Transaction.CATEGORY_TRANSFER)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(transactionDate)
                    .valueDate(transactionDate)
                    .initiatedBy(initiatedBy)
                    .approvedBy(initiatedBy)
                    .approvalDate(transactionDate)
                    .build();
            
            debitTransaction.setCreatedAt(transactionDate);
            debitTransaction.setUpdatedAt(transactionDate);
            Transaction savedDebitTransaction = transactionRepository.save(debitTransaction);
            
            // Create refund transaction
            LocalDateTime refundTime = transactionDate.plusMinutes(20);
            Transaction refundTransaction = Transaction.builder()
                    .account(sourceAccount)
                    .transactionType(Transaction.TYPE_CREDIT)
                    .amount(totalAmount)
                    .currency("INR")
                    .balanceBefore(sourceAccount.getBalance())
                    .balanceAfter(sourceAccount.getBalance().add(totalAmount))
                    .description("Refund - NEFT transfer failed")
                    .category(Transaction.CATEGORY_REFUND)
                    .status(Transaction.STATUS_COMPLETED)
                    .transactionDate(refundTime)
                    .initiatedBy("SYSTEM")
                    .approvedBy("SYSTEM")
                    .approvalDate(refundTime)
                    .build();
            
            refundTransaction.setCreatedAt(refundTime);
            refundTransaction.setUpdatedAt(refundTime);
            transactionRepository.save(refundTransaction);
            
            // Create the failed EFT transaction
            EFTTransaction eftTransaction = EFTTransaction.builder()
                    .eftType(EFTTransaction.TYPE_NEFT)
                    .sourceAccount(sourceAccount)
                    .beneficiary(beneficiary)
                    .beneficiaryAccountNumber(beneficiary.getAccountNumber())
                    .beneficiaryName(beneficiary.getBeneficiaryName())
                    .beneficiaryIfsc(beneficiary.getIfscCode())
                    .beneficiaryBankName(beneficiary.getBankName())
                    .amount(amount)
                    .charges(charges)
                    .totalAmount(totalAmount)
                    .currency("INR")
                    .purpose(purpose)
                    .remarks(remarks)
                    .status(EFTTransaction.STATUS_FAILED)
                    .batchId(batchId)
                    .batchTime(transactionDate.toLocalTime().withMinute(0).withSecond(0))
                    .estimatedCompletion(transactionDate.plusMinutes(30))
                    .actualCompletion(transactionDate.plusMinutes(20))
                    .initiatedBy(initiatedBy)
                    .processedBy("NEFT_BATCH_PROCESSOR")
                    .failureReason(failureReason)
                    .transaction(savedDebitTransaction)
                    .build();
            
            eftTransaction.setCreatedAt(transactionDate);
            eftTransaction.setUpdatedAt(transactionDate.plusMinutes(20));
            eftTransactionRepository.save(eftTransaction);
            
            log.debug("Created failed NEFT transaction: {} with refund", 
                eftTransaction.getEftReference());
            
        } catch (Exception e) {
            log.warn("Failed to create failed NEFT transaction: {}", e.getMessage());
        }
    }
}
