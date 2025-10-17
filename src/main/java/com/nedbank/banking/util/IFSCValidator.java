package com.nedbank.banking.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for IFSC code validation and bank details lookup
 */
public class IFSCValidator {

    // IFSC code pattern: 4 letters (bank code) + 0 + 6 alphanumeric (branch code)
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    // Sample bank code to bank name mapping (first 4 characters of IFSC)
    // In production, this would come from a database or external API
    private static final Map<String, String> BANK_CODES = new HashMap<>();

    static {
        // Major Indian banks
        BANK_CODES.put("SBIN", "State Bank of India");
        BANK_CODES.put("HDFC", "HDFC Bank");
        BANK_CODES.put("ICIC", "ICICI Bank");
        BANK_CODES.put("AXIS", "Axis Bank");
        BANK_CODES.put("PUNB", "Punjab National Bank");
        BANK_CODES.put("BARB", "Bank of Baroda");
        BANK_CODES.put("CNRB", "Canara Bank");
        BANK_CODES.put("UBIN", "Union Bank of India");
        BANK_CODES.put("INDB", "IndusInd Bank");
        BANK_CODES.put("IDIB", "Indian Bank");
        BANK_CODES.put("KKBK", "Kotak Mahindra Bank");
        BANK_CODES.put("YESB", "YES Bank");
        BANK_CODES.put("UTIB", "Axis Bank");
        BANK_CODES.put("IDFB", "IDFC First Bank");
        BANK_CODES.put("FDRL", "Federal Bank");
        BANK_CODES.put("KARB", "Karnataka Bank");
        BANK_CODES.put("MAHB", "Bank of Maharashtra");
        BANK_CODES.put("VIJB", "Vijaya Bank");
        BANK_CODES.put("CITI", "Citibank");
        BANK_CODES.put("SCBL", "Standard Chartered Bank");
        BANK_CODES.put("HSBC", "HSBC Bank");
        BANK_CODES.put("DBSS", "DBS Bank");
        BANK_CODES.put("DEUT", "Deutsche Bank");
        BANK_CODES.put("BKID", "Bank of India");
        BANK_CODES.put("UCBA", "UCO Bank");
        BANK_CODES.put("CBIN", "Central Bank of India");
        BANK_CODES.put("ALLA", "Allahabad Bank");
        BANK_CODES.put("ANDB", "Andhra Bank");
        
        // Add Nedbank for our system
        BANK_CODES.put("BOFP", "Bank of People");
    }

    /**
     * Validate IFSC code format
     */
    public static boolean isValidFormat(String ifscCode) {
        if (ifscCode == null || ifscCode.isBlank()) {
            return false;
        }
        return IFSC_PATTERN.matcher(ifscCode.trim().toUpperCase()).matches();
    }

    /**
     * Get bank name from IFSC code
     */
    public static String getBankName(String ifscCode) {
        if (!isValidFormat(ifscCode)) {
            return null;
        }
        String bankCode = ifscCode.substring(0, 4).toUpperCase();
        return BANK_CODES.getOrDefault(bankCode, "Unknown Bank");
    }

    /**
     * Validate IFSC code and return bank details
     */
    public static IFSCDetails validateAndGetDetails(String ifscCode) {
        if (!isValidFormat(ifscCode)) {
            throw new IllegalArgumentException("Invalid IFSC code format: " + ifscCode);
        }

        String bankCode = ifscCode.substring(0, 4).toUpperCase();
        String branchCode = ifscCode.substring(5).toUpperCase();
        String bankName = BANK_CODES.getOrDefault(bankCode, "Unknown Bank");

        // Check if bank is recognized
        if ("Unknown Bank".equals(bankName)) {
            throw new IllegalArgumentException("Bank code not recognized: " + bankCode);
        }

        return IFSCDetails.builder()
                .ifscCode(ifscCode.toUpperCase())
                .bankCode(bankCode)
                .branchCode(branchCode)
                .bankName(bankName)
                .isValid(true)
                .build();
    }

    /**
     * Check if IFSC code belongs to a specific bank
     */
    public static boolean isBank(String ifscCode, String bankCode) {
        if (!isValidFormat(ifscCode)) {
            return false;
        }
        return ifscCode.substring(0, 4).equalsIgnoreCase(bankCode);
    }

    /**
     * DTO for IFSC details
     */
    public static class IFSCDetails {
        private String ifscCode;
        private String bankCode;
        private String branchCode;
        private String bankName;
        private boolean isValid;

        private IFSCDetails(Builder builder) {
            this.ifscCode = builder.ifscCode;
            this.bankCode = builder.bankCode;
            this.branchCode = builder.branchCode;
            this.bankName = builder.bankName;
            this.isValid = builder.isValid;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getIfscCode() { return ifscCode; }
        public String getBankCode() { return bankCode; }
        public String getBranchCode() { return branchCode; }
        public String getBankName() { return bankName; }
        public boolean isValid() { return isValid; }

        public static class Builder {
            private String ifscCode;
            private String bankCode;
            private String branchCode;
            private String bankName;
            private boolean isValid;

            public Builder ifscCode(String ifscCode) {
                this.ifscCode = ifscCode;
                return this;
            }

            public Builder bankCode(String bankCode) {
                this.bankCode = bankCode;
                return this;
            }

            public Builder branchCode(String branchCode) {
                this.branchCode = branchCode;
                return this;
            }

            public Builder bankName(String bankName) {
                this.bankName = bankName;
                return this;
            }

            public Builder isValid(boolean isValid) {
                this.isValid = isValid;
                return this;
            }

            public IFSCDetails build() {
                return new IFSCDetails(this);
            }
        }
    }
}

