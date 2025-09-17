package com.nedbank.banking.config;

import com.nedbank.banking.entity.*;
import com.nedbank.banking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final VasProviderRepository vasProviderRepository;
    private final TransactionRepository transactionRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🚀 Starting comprehensive data seeding for Online Banking System...");
        
        seedPermissions();
        seedRoles();
        seedCustomers();
        seedUsers();
        seedAccounts();
        seedVasProviders();
        seedSampleTransactions();
        seedSampleLoans();
        
        log.info("✅ Data seeding completed successfully! All modules ready for testing.");
    }

    private void seedPermissions() {
        log.info("🔐 Seeding permissions for Online Banking System...");
        
        String[] permissions = {
            // Account Management
            "VIEW_ALL_ACCOUNTS", "CREATE_ACCOUNT", "DELETE_ACCOUNT", "VIEW_OWN_ACCOUNT", "MODIFY_ACCOUNT",
            
            // Transaction Management
            "TRANSFER_MONEY", "VIEW_TRANSACTIONS", "VIEW_ALL_TRANSACTIONS", "BULK_UPLOAD_TRANSACTIONS",
            
            // Loan Management
            "APPLY_LOAN", "APPROVE_LOANS", "REJECT_LOANS", "VIEW_LOAN_APPLICATIONS", "MANAGE_LOANS",
            
            // Payment & VAS
            "PROCESS_PAYMENTS", "EFT_PAYMENTS", "UPI_PAYMENTS", "QR_PAYMENTS", "GENERATE_QR_CODES",
            "MOBILE_RECHARGE", "DTH_RECHARGE", "ELECTRICITY_BILL_PAYMENT",
            
            // Reports & Statements
            "GENERATE_PDF_STATEMENTS", "EMAIL_STATEMENTS", "VIEW_REPORTS", "GENERATE_REPORTS",
            
            // User & Permission Management
            "MANAGE_USERS", "MANAGE_USER_PERMISSIONS", "READ_ACCESS", "WRITE_ACCESS", "DELETE_ACCESS",
            
            // System Administration
            "VIEW_AUDIT_LOGS", "SYSTEM_CONFIGURATION", "MANAGE_ROLES", "MANAGE_VAS_PROVIDERS",
            
            // Dashboard Access
            "CUSTOMER_DASHBOARD", "ACCOUNTANT_DASHBOARD", "ADMIN_DASHBOARD", "SUPERADMIN_DASHBOARD"
        };
        
        for (String permName : permissions) {
            if (!permissionRepository.existsByName(permName)) {
                Permission permission = Permission.builder()
                    .name(permName)
                    .description("Permission to " + permName.replace("_", " ").toLowerCase())
                    .build();
                permissionRepository.save(permission);
                log.debug("✅ Created permission: {}", permName);
            }
        }
        log.info("📋 Seeded {} permissions", permissions.length);
    }

    private void seedRoles() {
        log.info("👥 Seeding roles for Online Banking System...");
        
        // SUPERADMIN Role - Manages user permissions (Read/Write access)
        if (!roleRepository.existsByName("SUPERADMIN")) {
            Role superadminRole = Role.builder()
                .name("SUPERADMIN")
                .description("Super Administrator - Manages user permissions and system configuration")
                .build();
            
            // Superadmin gets ALL permissions
            Set<Permission> allPermissions = Set.copyOf(permissionRepository.findAll());
            superadminRole.setPermissions(allPermissions);
            
            roleRepository.save(superadminRole);
            log.debug("✅ Created SUPERADMIN role with all permissions");
        }
        
        // ADMIN Role - System administration
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = Role.builder()
                .name("ADMIN")
                .description("System Administrator with comprehensive access")
                .build();
            
            Set<Permission> adminPermissions = Set.of(
                permissionRepository.findByName("VIEW_ALL_ACCOUNTS").orElseThrow(),
                permissionRepository.findByName("CREATE_ACCOUNT").orElseThrow(),
                permissionRepository.findByName("MODIFY_ACCOUNT").orElseThrow(),
                permissionRepository.findByName("VIEW_ALL_TRANSACTIONS").orElseThrow(),
                permissionRepository.findByName("APPROVE_LOANS").orElseThrow(),
                permissionRepository.findByName("REJECT_LOANS").orElseThrow(),
                permissionRepository.findByName("MANAGE_LOANS").orElseThrow(),
                permissionRepository.findByName("GENERATE_PDF_STATEMENTS").orElseThrow(),
                permissionRepository.findByName("EMAIL_STATEMENTS").orElseThrow(),
                permissionRepository.findByName("VIEW_REPORTS").orElseThrow(),
                permissionRepository.findByName("GENERATE_REPORTS").orElseThrow(),
                permissionRepository.findByName("VIEW_AUDIT_LOGS").orElseThrow(),
                permissionRepository.findByName("MANAGE_VAS_PROVIDERS").orElseThrow(),
                permissionRepository.findByName("ADMIN_DASHBOARD").orElseThrow()
            );
            adminRole.setPermissions(adminPermissions);
            
            roleRepository.save(adminRole);
            log.debug("✅ Created ADMIN role");
        }
        
        // ACCOUNTANT Role - Bulk upload transactions, financial operations
        if (!roleRepository.existsByName("ACCOUNTANT")) {
            Role accountantRole = Role.builder()
                .name("ACCOUNTANT")
                .description("Accountant - Can bulk upload transaction files and manage financial operations")
                .build();
            
            Set<Permission> accountantPermissions = Set.of(
                permissionRepository.findByName("VIEW_ALL_ACCOUNTS").orElseThrow(),
                permissionRepository.findByName("CREATE_ACCOUNT").orElseThrow(),
                permissionRepository.findByName("VIEW_ALL_TRANSACTIONS").orElseThrow(),
                permissionRepository.findByName("BULK_UPLOAD_TRANSACTIONS").orElseThrow(),
                permissionRepository.findByName("GENERATE_PDF_STATEMENTS").orElseThrow(),
                permissionRepository.findByName("EMAIL_STATEMENTS").orElseThrow(),
                permissionRepository.findByName("VIEW_REPORTS").orElseThrow(),
                permissionRepository.findByName("GENERATE_REPORTS").orElseThrow(),
                permissionRepository.findByName("PROCESS_PAYMENTS").orElseThrow(),
                permissionRepository.findByName("ACCOUNTANT_DASHBOARD").orElseThrow()
            );
            accountantRole.setPermissions(accountantPermissions);
            
            roleRepository.save(accountantRole);
            log.debug("✅ Created ACCOUNTANT role");
        }
        
        // CUSTOMER Role - Bank customers with self-service access
        if (!roleRepository.existsByName("CUSTOMER")) {
            Role customerRole = Role.builder()
                .name("CUSTOMER")
                .description("Bank Customer - Can view own accounts, make transactions, and use online services")
                .build();
            
            Set<Permission> customerPermissions = Set.of(
                permissionRepository.findByName("VIEW_OWN_ACCOUNT").orElseThrow(),
                permissionRepository.findByName("VIEW_TRANSACTIONS").orElseThrow(),
                permissionRepository.findByName("TRANSFER_MONEY").orElseThrow(),
                permissionRepository.findByName("APPLY_LOAN").orElseThrow(),
                permissionRepository.findByName("PROCESS_PAYMENTS").orElseThrow(),
                permissionRepository.findByName("EFT_PAYMENTS").orElseThrow(),
                permissionRepository.findByName("UPI_PAYMENTS").orElseThrow(),
                permissionRepository.findByName("QR_PAYMENTS").orElseThrow(),
                permissionRepository.findByName("MOBILE_RECHARGE").orElseThrow(),
                permissionRepository.findByName("DTH_RECHARGE").orElseThrow(),
                permissionRepository.findByName("ELECTRICITY_BILL_PAYMENT").orElseThrow(),
                permissionRepository.findByName("CUSTOMER_DASHBOARD").orElseThrow()
            );
            customerRole.setPermissions(customerPermissions);
            
            roleRepository.save(customerRole);
            log.debug("✅ Created CUSTOMER role");
        }
        
        log.info("👥 Seeded 4 roles: SUPERADMIN, ADMIN, ACCOUNTANT, CUSTOMER");
    }

    private void seedCustomers() {
        log.info("Seeding customers...");
        
        // Individual Customer
        if (!customerRepository.existsByCustomerUid("CUST-001")) {
            Customer individual = Customer.builder()
                .customerUid("CUST-001")
                .name("Rajesh Kumar")
                .dob(LocalDate.of(1990, 5, 15))
                .email("rajesh.kumar@email.com")
                .mobile("9876543210")
                .customerType("INDIVIDUAL")
                .kycStatus("APPROVED")
                .build();
            customerRepository.save(individual);
            log.debug("Created individual customer: Rajesh Kumar");
        }
        
        // Corporate Customer
        if (!customerRepository.existsByCustomerUid("CORP-001")) {
            Customer corporate = Customer.builder()
                .customerUid("CORP-001")
                .name("USR India Pvt Ltd")
                .email("finance@usr.co.in")
                .mobile("1800123456")
                .customerType("CORPORATE")
                .kycStatus("APPROVED")
                .build();
            customerRepository.save(corporate);
            log.debug("Created corporate customer: Spotify India Pvt Ltd");
        }
    }

    private void seedUsers() {
        log.info("👤 Seeding users for each role...");
        
        // SUPERADMIN User
        if (!userRepository.existsByUsername("superadmin@nedbank.com")) {
            Role superadminRole = roleRepository.findByName("SUPERADMIN").orElseThrow();
            
            User superadmin = User.builder()
                .username("superadmin@nedbank.com")
                .email("superadmin@nedbank.com")
                .mobile("9000000000")
                .passwordHash("$2a$10$dummyHashForNow") // TODO: Replace with actual BCrypt hash
                .status("ACTIVE")
                .roles(Set.of(superadminRole))
                .build();
            userRepository.save(superadmin);
            log.debug("✅ Created SUPERADMIN user: superadmin@nedbank.com");
        }
        
        // ADMIN User
        if (!userRepository.existsByUsername("admin@nedbank.com")) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            
            User admin = User.builder()
                .username("admin@nedbank.com")
                .email("admin@nedbank.com")
                .mobile("9111111111")
                .passwordHash("$2a$10$dummyHashForNow") // TODO: Replace with actual BCrypt hash
                .status("ACTIVE")
                .roles(Set.of(adminRole))
                .build();
            userRepository.save(admin);
            log.debug("✅ Created ADMIN user: admin@nedbank.com");
        }
        
        // ACCOUNTANT User
        if (!userRepository.existsByUsername("accountant@nedbank.com")) {
            Role accountantRole = roleRepository.findByName("ACCOUNTANT").orElseThrow();
            
            User accountant = User.builder()
                .username("accountant@nedbank.com")
                .email("accountant@nedbank.com")
                .mobile("9222222222")
                .passwordHash("$2a$10$dummyHashForNow") // TODO: Replace with actual BCrypt hash
                .status("ACTIVE")
                .roles(Set.of(accountantRole))
                .build();
            userRepository.save(accountant);
            log.debug("✅ Created ACCOUNTANT user: accountant@nedbank.com");
        }
        
        // CUSTOMER User (Individual)
        if (!userRepository.existsByUsername("rajesh.kumar@email.com")) {
            Customer customer = customerRepository.findByCustomerUid("CUST-001").orElseThrow();
            Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
            
            User customerUser = User.builder()
                .username("rajesh.kumar@email.com")
                .email("rajesh.kumar@email.com")
                .mobile("9876543210")
                .passwordHash("$2a$10$dummyHashForNow") // TODO: Replace with actual BCrypt hash
                .status("ACTIVE")
                .customer(customer)
                .roles(Set.of(customerRole))
                .build();
            userRepository.save(customerUser);
            log.debug("✅ Created CUSTOMER user: rajesh.kumar@email.com");
        }
        
        // CUSTOMER User (Corporate)
        if (!userRepository.existsByUsername("finance@usr.co.in")) {
            Customer corporate = customerRepository.findByCustomerUid("CORP-001").orElseThrow();
            Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
            
            User corporateUser = User.builder()
                .username("finance@usr.co.in")
                .email("finance@usr.co.in")
                .mobile("1800123456")
                .passwordHash("$2a$10$dummyHashForNow") // TODO: Replace with actual BCrypt hash
                .status("ACTIVE")
                .customer(corporate)
                .roles(Set.of(customerRole))
                .build();
            userRepository.save(corporateUser);
            log.debug("✅ Created CUSTOMER user (Corporate): finance@usr.co.in");
        }
        
        log.info("👤 Seeded 5 users: SUPERADMIN, ADMIN, ACCOUNTANT, 2 CUSTOMERS");
    }

    private void seedAccounts() {
        log.info("Seeding accounts...");
        
        // Individual Customer Account
        if (!accountRepository.existsByAccountNumber("ACC-001-SAVINGS")) {
            Customer individual = customerRepository.findByCustomerUid("CUST-001").orElseThrow();
            
            Account savingsAccount = Account.builder()
                .accountNumber("ACC-001-SAVINGS")
                .accountType("SAVINGS")
                .currency("INR")
                .balance(new BigDecimal("50000.00"))
                .status("ACTIVE")
                .customer(individual)
                .build();
            accountRepository.save(savingsAccount);
            log.debug("Created savings account for individual customer");
        }
        
        // Corporate Customer Account
        if (!accountRepository.existsByAccountNumber("ACC-CORP-001-CURRENT")) {
            Customer corporate = customerRepository.findByCustomerUid("CORP-001").orElseThrow();
            
            Account currentAccount = Account.builder()
                .accountNumber("ACC-CORP-001-CURRENT")
                .accountType("CURRENT")
                .currency("INR")
                .balance(new BigDecimal("5000000.00"))
                .status("ACTIVE")
                .customer(corporate)
                .build();
            accountRepository.save(currentAccount);
            log.debug("Created current account for corporate customer");
        }
    }

    private void seedVasProviders() {
        log.info("🏪 Seeding VAS providers for Value-Added Services...");
        
        // Mobile Recharge Providers
        String[][] mobileProviders = {
            {"Airtel India", "MOBILE_RECHARGE", "https://api.airtel.in/recharge"},
            {"Jio", "MOBILE_RECHARGE", "https://api.jio.com/recharge"},
            {"BSNL", "MOBILE_RECHARGE", "https://api.bsnl.co.in/recharge"},
            {"Vi (Vodafone Idea)", "MOBILE_RECHARGE", "https://api.myvi.in/recharge"}
        };
        
        for (String[] provider : mobileProviders) {
            if (!vasProviderRepository.existsByProviderName(provider[0])) {
                VasProvider vasProvider = VasProvider.builder()
                    .providerName(provider[0])
                    .serviceType(provider[1])
                    .apiUrl(provider[2])
                    .active(true)
                    .build();
                vasProviderRepository.save(vasProvider);
                log.debug("✅ Created mobile recharge provider: {}", provider[0]);
            }
        }
        
        // DTH Providers
        String[][] dthProviders = {
            {"Tata Sky", "DTH", "https://api.tatasky.com/recharge"},
            {"Dish TV", "DTH", "https://api.dishtv.in/recharge"},
            {"Airtel Digital TV", "DTH", "https://api.airtel.in/dth"},
            {"Sun Direct", "DTH", "https://api.sundirect.in/recharge"}
        };
        
        for (String[] provider : dthProviders) {
            if (!vasProviderRepository.existsByProviderName(provider[0])) {
                VasProvider vasProvider = VasProvider.builder()
                    .providerName(provider[0])
                    .serviceType(provider[1])
                    .apiUrl(provider[2])
                    .active(true)
                    .build();
                vasProviderRepository.save(vasProvider);
                log.debug("✅ Created DTH provider: {}", provider[0]);
            }
        }
        
        // Electricity Bill Payment Providers
        String[][] electricityProviders = {
            {"BESCOM", "ELECTRICITY", "https://api.bescom.gov.in/billpay"},
            {"MSEB", "ELECTRICITY", "https://api.msedcl.com/billpay"},
            {"KSEB", "ELECTRICITY", "https://api.kseb.in/billpay"},
            {"TNEB", "ELECTRICITY", "https://api.tneb.gov.in/billpay"}
        };
        
        for (String[] provider : electricityProviders) {
            if (!vasProviderRepository.existsByProviderName(provider[0])) {
                VasProvider vasProvider = VasProvider.builder()
                    .providerName(provider[0])
                    .serviceType(provider[1])
                    .apiUrl(provider[2])
                    .active(true)
                    .build();
                vasProviderRepository.save(vasProvider);
                log.debug("✅ Created electricity provider: {}", provider[0]);
            }
        }
        
        log.info("🏪 Seeded 12 VAS providers: 4 Mobile, 4 DTH, 4 Electricity");
    }

    private void seedSampleTransactions() {
        log.info("💳 Seeding sample transactions...");
        
        Account individualAccount = accountRepository.findByAccountNumber("ACC-001-SAVINGS").orElse(null);
        Account corporateAccount = accountRepository.findByAccountNumber("ACC-CORP-001-CURRENT").orElse(null);
        User customerUser = userRepository.findByUsername("rajesh.kumar@email.com").orElse(null);
        User accountantUser = userRepository.findByUsername("accountant@nedbank.com").orElse(null);
        
        if (individualAccount != null && customerUser != null) {
            // Sample credit transaction
            Transaction creditTxn = Transaction.builder()
                .account(individualAccount)
                .txnType("CREDIT")
                .amount(new BigDecimal("25000.00"))
                .balanceAfter(new BigDecimal("75000.00"))
                .reference("SAL-001-MAR24")
                .narration("Salary credit for March 2024")
                .status("SUCCESS")
                .createdBy(accountantUser)
                .build();
            transactionRepository.save(creditTxn);
            
            // Sample debit transaction
            Transaction debitTxn = Transaction.builder()
                .account(individualAccount)
                .txnType("DEBIT")
                .amount(new BigDecimal("5000.00"))
                .balanceAfter(new BigDecimal("70000.00"))
                .reference("UPI-001")
                .narration("UPI payment to John Doe")
                .status("SUCCESS")
                .createdBy(customerUser)
                .build();
            transactionRepository.save(debitTxn);
            
            log.debug("✅ Created sample transactions for individual account");
        }
        
        if (corporateAccount != null && accountantUser != null) {
            // Corporate payroll transaction
            Transaction payrollTxn = Transaction.builder()
                .account(corporateAccount)
                .txnType("DEBIT")
                .amount(new BigDecimal("500000.00"))
                .balanceAfter(new BigDecimal("4500000.00"))
                .reference("PAYROLL-MAR24")
                .narration("Employee salary disbursement - March 2024")
                .status("SUCCESS")
                .createdBy(accountantUser)
                .build();
            transactionRepository.save(payrollTxn);
            
            log.debug("✅ Created sample transactions for corporate account");
        }
        
        log.info("💳 Seeded sample transactions for testing");
    }

    private void seedSampleLoans() {
        log.info("🏠 Seeding sample loan applications...");
        
        Customer individual = customerRepository.findByCustomerUid("CUST-001").orElse(null);
        Customer corporate = customerRepository.findByCustomerUid("CORP-001").orElse(null);
        
        if (individual != null) {
            // Home Loan Application
            LoanApplication homeLoan = LoanApplication.builder()
                .customer(individual)
                .loanType("HOME")
                .amount(new BigDecimal("2500000.00")) // 25 lakh
                .tenureMonths(240) // 20 years
                .interestRate(new BigDecimal("8.50"))
                .status("APPLIED")
                .build();
            loanApplicationRepository.save(homeLoan);
            
            // Personal Loan Application
            LoanApplication personalLoan = LoanApplication.builder()
                .customer(individual)
                .loanType("PERSONAL")
                .amount(new BigDecimal("500000.00")) // 5 lakh
                .tenureMonths(60) // 5 years
                .interestRate(new BigDecimal("12.50"))
                .status("APPROVED")
                .build();
            loanApplicationRepository.save(personalLoan);
            
            log.debug("✅ Created loan applications for individual customer");
        }
        
        if (corporate != null) {
            // Business Loan Application
            LoanApplication businessLoan = LoanApplication.builder()
                .customer(corporate)
                .loanType("BUSINESS")
                .amount(new BigDecimal("10000000.00")) // 1 crore
                .tenureMonths(120) // 10 years
                .interestRate(new BigDecimal("9.75"))
                .status("UNDER_REVIEW")
                .build();
            loanApplicationRepository.save(businessLoan);
            
            log.debug("✅ Created loan application for corporate customer");
        }
        
        log.info("🏠 Seeded sample loan applications: HOME, PERSONAL, BUSINESS");
    }
}
