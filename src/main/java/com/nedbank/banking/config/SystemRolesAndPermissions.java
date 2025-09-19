package com.nedbank.banking.config;

/**
 * Defines the Banking System Roles and Permissions
 * 
 * ROLES:
 * - Customer: Bank customers who can view their accounts and transactions
 * - Accountant: Bank staff who can process transactions and view customer accounts  
 * - Admin: Bank administrators who can manage users and system settings
 * - Superadmin: System administrators who can manage all permissions and access
 * 
 * PERMISSIONS:
 * - READ_OWN_ACCOUNTS: View own account details and transactions
 * - READ_ALL_ACCOUNTS: View all customer accounts (staff only)
 * - WRITE_TRANSACTIONS: Create and process transactions
 * - READ_CUSTOMERS: View customer information  
 * - WRITE_CUSTOMERS: Create and update customer information
 * - READ_USERS: View user accounts
 * - WRITE_USERS: Create and update user accounts
 * - MANAGE_PERMISSIONS: Assign/revoke permissions (Superadmin only)
 * - SYSTEM_ADMINISTRATION: Full system access
 * 
 * ROLE-PERMISSION MAPPING:
 * 
 * Customer:
 * - READ_OWN_ACCOUNTS
 * 
 * Accountant:  
 * - READ_OWN_ACCOUNTS
 * - READ_ALL_ACCOUNTS
 * - WRITE_TRANSACTIONS
 * - READ_CUSTOMERS
 * - WRITE_CUSTOMERS
 * 
 * Admin:
 * - READ_OWN_ACCOUNTS  
 * - READ_ALL_ACCOUNTS
 * - WRITE_TRANSACTIONS
 * - READ_CUSTOMERS
 * - WRITE_CUSTOMERS
 * - READ_USERS
 * - WRITE_USERS
 * 
 * Superadmin:
 * - All permissions above
 * - MANAGE_PERMISSIONS
 * - SYSTEM_ADMINISTRATION
 */
public class SystemRolesAndPermissions {

    // Role names
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_ACCOUNTANT = "ACCOUNTANT"; 
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUPERADMIN = "SUPERADMIN";

    // Permission names
    public static final String PERM_READ_OWN_ACCOUNTS = "READ_OWN_ACCOUNTS";
    public static final String PERM_READ_ALL_ACCOUNTS = "READ_ALL_ACCOUNTS";
    public static final String PERM_WRITE_TRANSACTIONS = "WRITE_TRANSACTIONS";
    public static final String PERM_READ_CUSTOMERS = "READ_CUSTOMERS";
    public static final String PERM_WRITE_CUSTOMERS = "WRITE_CUSTOMERS";
    public static final String PERM_READ_USERS = "READ_USERS";
    public static final String PERM_WRITE_USERS = "WRITE_USERS";
    public static final String PERM_MANAGE_PERMISSIONS = "MANAGE_PERMISSIONS";
    public static final String PERM_SYSTEM_ADMINISTRATION = "SYSTEM_ADMINISTRATION";
}
