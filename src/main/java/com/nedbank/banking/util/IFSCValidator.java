package com.nedbank.banking.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.regex.Pattern;

/**
 * Utility class for IFSC code validation and bank details lookup
 */
public class IFSCValidator {

    // IFSC code pattern: 4 letters (bank code) + 0 + 6 alphanumeric (branch code)
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    
    // IFSC API endpoint
    private static final String IFSC_API_URL = "https://ifsc.razorpay.com/";
    
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        try {
            IFSCDetails details = validateAndGetDetails(ifscCode);
            return details.getBank();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validate IFSC code and return bank details
     */
    public static IFSCDetails validateAndGetDetails(String ifscCode) {
        if (!isValidFormat(ifscCode)) {
            throw new IllegalArgumentException("Invalid IFSC code format: " + ifscCode);
        }

        try {
            String url = IFSC_API_URL + ifscCode.toUpperCase();
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null || response.isBlank()) {
                throw new IllegalArgumentException("IFSC code not found: " + ifscCode);
            }
            
            // Parse JSON response
            IFSCDetails details = objectMapper.readValue(response, IFSCDetails.class);
            return details;
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("IFSC code not found: " + ifscCode);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error validating IFSC code: " + e.getMessage());
        }
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
        @JsonProperty("BRANCH")
        private String branch;
        
        @JsonProperty("CENTRE")
        private String centre;
        
        @JsonProperty("DISTRICT")
        private String district;
        
        @JsonProperty("STATE")
        private String state;
        
        @JsonProperty("ADDRESS")
        private String address;
        
        @JsonProperty("CONTACT")
        private String contact;
        
        @JsonProperty("IMPS")
        private Boolean imps;
        
        @JsonProperty("CITY")
        private String city;
        
        @JsonProperty("UPI")
        private Boolean upi;
        
        @JsonProperty("MICR")
        private String micr;
        
        @JsonProperty("RTGS")
        private Boolean rtgs;
        
        @JsonProperty("NEFT")
        private Boolean neft;
        
        @JsonProperty("SWIFT")
        private String swift;
        
        @JsonProperty("ISO3166")
        private String iso3166;
        
        @JsonProperty("BANK")
        private String bank;
        
        @JsonProperty("BANKCODE")
        private String bankCode;
        
        @JsonProperty("IFSC")
        private String ifsc;

        // Default constructor for Jackson
        public IFSCDetails() {
        }

        // Getters
        public String getBranch() { return branch; }
        public String getCentre() { return centre; }
        public String getDistrict() { return district; }
        public String getState() { return state; }
        public String getAddress() { return address; }
        public String getContact() { return contact; }
        public Boolean getImps() { return imps; }
        public String getCity() { return city; }
        public Boolean getUpi() { return upi; }
        public String getMicr() { return micr; }
        public Boolean getRtgs() { return rtgs; }
        public Boolean getNeft() { return neft; }
        public String getSwift() { return swift; }
        public String getIso3166() { return iso3166; }
        public String getBank() { return bank; }
        public String getBankCode() { return bankCode; }
        public String getIfsc() { return ifsc; }
        
        // For backward compatibility
        public String getIfscCode() { return ifsc; }
        public String getBankName() { return bank; }
        public boolean isValid() { return ifsc != null && !ifsc.isBlank(); }
        public String getBranchCode() { 
            return ifsc != null && ifsc.length() > 5 ? ifsc.substring(5) : null; 
        }

        // Setters (for Jackson deserialization)
        public void setBranch(String branch) { this.branch = branch; }
        public void setCentre(String centre) { this.centre = centre; }
        public void setDistrict(String district) { this.district = district; }
        public void setState(String state) { this.state = state; }
        public void setAddress(String address) { this.address = address; }
        public void setContact(String contact) { this.contact = contact; }
        public void setImps(Boolean imps) { this.imps = imps; }
        public void setCity(String city) { this.city = city; }
        public void setUpi(Boolean upi) { this.upi = upi; }
        public void setMicr(String micr) { this.micr = micr; }
        public void setRtgs(Boolean rtgs) { this.rtgs = rtgs; }
        public void setNeft(Boolean neft) { this.neft = neft; }
        public void setSwift(String swift) { this.swift = swift; }
        public void setIso3166(String iso3166) { this.iso3166 = iso3166; }
        public void setBank(String bank) { this.bank = bank; }
        public void setBankCode(String bankCode) { this.bankCode = bankCode; }
        public void setIfsc(String ifsc) { this.ifsc = ifsc; }
    }
}

