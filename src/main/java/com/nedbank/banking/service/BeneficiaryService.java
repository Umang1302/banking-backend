package com.nedbank.banking.service;

import com.nedbank.banking.dto.BeneficiaryRequest;
import com.nedbank.banking.dto.BeneficiaryResponse;
import com.nedbank.banking.entity.Beneficiary;
import com.nedbank.banking.entity.Customer;
import com.nedbank.banking.entity.User;
import com.nedbank.banking.repository.BeneficiaryRepository;
import com.nedbank.banking.repository.CustomerRepository;
import com.nedbank.banking.repository.UserRepository;
import com.nedbank.banking.util.IFSCValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing beneficiaries (external bank accounts)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    /**
     * Add a new beneficiary
     */
    @Transactional
    public BeneficiaryResponse addBeneficiary(BeneficiaryRequest request) {
        User currentUser = getCurrentUser();
        Customer customer = getCustomerForUser(currentUser);

        log.info("User {} adding beneficiary: {}", currentUser.getUsername(), request.getBeneficiaryName());

        // Validate IFSC code and get bank details
        IFSCValidator.IFSCDetails ifscDetails = IFSCValidator.validateAndGetDetails(request.getIfscCode());

        // Check if beneficiary already exists
        if (beneficiaryRepository.existsByCustomerIdAndAccountNumberAndIfscCode(
                customer.getId(), request.getAccountNumber(), request.getIfscCode())) {
            throw new IllegalArgumentException("Beneficiary with this account number and IFSC code already exists");
        }

        // Auto-fill bank name if not provided
        String bankName = request.getBankName() != null && !request.getBankName().isBlank()
                ? request.getBankName()
                : ifscDetails.getBankName();

        // Create beneficiary (pending admin verification)
        Beneficiary beneficiary = Beneficiary.builder()
                .customer(customer)
                .beneficiaryName(request.getBeneficiaryName())
                .accountNumber(request.getAccountNumber())
                .ifscCode(request.getIfscCode().toUpperCase())
                .bankName(bankName)
                .branchName(request.getBranchName())
                .nickname(request.getNickname())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .isVerified(false) // Will be verified by admin
                .status(Beneficiary.STATUS_PENDING_VERIFICATION) // Requires admin approval
                .addedBy(currentUser.getUsername())
                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary added successfully. ID: {}, Name: {}", saved.getId(), saved.getBeneficiaryName());

        return mapToResponse(saved);
    }

    /**
     * Get all beneficiaries for current user (excludes inactive/deleted ones)
     */
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getAllBeneficiaries() {
        User currentUser = getCurrentUser();
        Customer customer = getCustomerForUser(currentUser);

        log.debug("Fetching beneficiaries for customer: {}", customer.getId());

        List<Beneficiary> beneficiaries = beneficiaryRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        return beneficiaries.stream()
                .filter(b -> !Beneficiary.STATUS_INACTIVE.equals(b.getStatus())) // Exclude deleted beneficiaries
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active beneficiaries for current user
     */
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getActiveBeneficiaries() {
        User currentUser = getCurrentUser();
        Customer customer = getCustomerForUser(currentUser);

        log.debug("Fetching active beneficiaries for customer: {}", customer.getId());

        List<Beneficiary> beneficiaries = beneficiaryRepository
                .findByCustomerIdAndStatusOrderByCreatedAtDesc(customer.getId(), Beneficiary.STATUS_ACTIVE);
        return beneficiaries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get beneficiary by ID
     */
    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(Long beneficiaryId) {
        User currentUser = getCurrentUser();
        Customer customer = getCustomerForUser(currentUser);

        Beneficiary beneficiary = beneficiaryRepository.findByIdAndCustomerId(beneficiaryId, customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found or access denied"));

        return mapToResponse(beneficiary);
    }

    /**
     * Update beneficiary
     */
    @Transactional
    public BeneficiaryResponse updateBeneficiary(Long beneficiaryId, BeneficiaryRequest request) {
        User currentUser = getCurrentUser();
        Customer customer = getCustomerForUser(currentUser);

        log.info("User {} updating beneficiary: {}", currentUser.getUsername(), beneficiaryId);

        Beneficiary beneficiary = beneficiaryRepository.findByIdAndCustomerId(beneficiaryId, customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found or access denied"));

        // Validate IFSC code
        IFSCValidator.IFSCDetails ifscDetails = IFSCValidator.validateAndGetDetails(request.getIfscCode());

        // Update fields
        beneficiary.setBeneficiaryName(request.getBeneficiaryName());
        beneficiary.setAccountNumber(request.getAccountNumber());
        beneficiary.setIfscCode(request.getIfscCode().toUpperCase());
        beneficiary.setBankName(request.getBankName() != null ? request.getBankName() : ifscDetails.getBankName());
        beneficiary.setBranchName(request.getBranchName());
        beneficiary.setNickname(request.getNickname());
        beneficiary.setEmail(request.getEmail());
        beneficiary.setMobile(request.getMobile());
        // Mark as unverified and pending verification after update (requires admin re-approval)
        beneficiary.setIsVerified(false);
        beneficiary.setStatus(Beneficiary.STATUS_PENDING_VERIFICATION);

        Beneficiary updated = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary updated successfully: {}", beneficiaryId);

        return mapToResponse(updated);
    }

    /**
     * Delete/deactivate beneficiary
     */
    @Transactional
    public void deleteBeneficiary(Long beneficiaryId) {
        User currentUser = getCurrentUser();
        Customer customer = getCustomerForUser(currentUser);

        log.info("User {} deleting beneficiary: {}", currentUser.getUsername(), beneficiaryId);

        Beneficiary beneficiary = beneficiaryRepository.findByIdAndCustomerId(beneficiaryId, customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found or access denied"));

        // Soft delete - mark as inactive
        beneficiary.setStatus(Beneficiary.STATUS_INACTIVE);
        beneficiaryRepository.save(beneficiary);

        log.info("Beneficiary deleted successfully: {}", beneficiaryId);
    }

    /**
     * Validate beneficiary for transfer (internal method)
     */
    @Transactional
    public Beneficiary validateBeneficiaryForTransfer(Long beneficiaryId, Long customerId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndCustomerId(beneficiaryId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found or access denied"));

        if (!beneficiary.isActive()) {
            throw new IllegalArgumentException("Beneficiary is not active");
        }

        if (beneficiary.isBlocked()) {
            throw new IllegalArgumentException("Beneficiary is blocked");
        }

        // Update last used timestamp
        beneficiary.markAsUsed();
        beneficiaryRepository.save(beneficiary);

        return beneficiary;
    }

    /**
     * Get all beneficiaries (Admin only)
     */
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getAllBeneficiariesAdmin() {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("ACCOUNT_READ")) {
            throw new SecurityException("Access denied");
        }

        log.debug("Admin fetching all beneficiaries");
        List<Beneficiary> beneficiaries = beneficiaryRepository.findAllBeneficiaries();
        return beneficiaries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get pending beneficiaries for approval (Admin only)
     */
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getPendingBeneficiariesAdmin() {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("ACCOUNT_READ")) {
            throw new SecurityException("Access denied");
        }

        log.debug("Admin fetching pending beneficiaries");
        List<Beneficiary> beneficiaries = beneficiaryRepository
                .findByStatusOrderByCreatedAtAsc(Beneficiary.STATUS_PENDING_VERIFICATION);
        return beneficiaries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Approve beneficiary (Admin only)
     */
    @Transactional
    public BeneficiaryResponse approveBeneficiary(Long beneficiaryId) {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("ACCOUNT_WRITE")) {
            throw new SecurityException("Access denied");
        }

        log.info("Admin {} approving beneficiary: {}", currentUser.getUsername(), beneficiaryId);

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found: " + beneficiaryId));

        if (!Beneficiary.STATUS_PENDING_VERIFICATION.equals(beneficiary.getStatus())) {
            throw new IllegalArgumentException("Beneficiary is not pending verification. Current status: " + beneficiary.getStatus());
        }

        // Approve beneficiary
        beneficiary.setStatus(Beneficiary.STATUS_ACTIVE);
        beneficiary.setIsVerified(true);
        beneficiary.setVerifiedBy(currentUser.getUsername());
        beneficiary.setVerifiedAt(LocalDateTime.now());

        Beneficiary updated = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary {} approved successfully by {}", beneficiaryId, currentUser.getUsername());

        return mapToResponse(updated);
    }

    /**
     * Reject beneficiary (Admin only)
     */
    @Transactional
    public void rejectBeneficiary(Long beneficiaryId, String reason) {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("ACCOUNT_WRITE")) {
            throw new SecurityException("Access denied");
        }

        log.info("Admin {} rejecting beneficiary: {}", currentUser.getUsername(), beneficiaryId);

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found: " + beneficiaryId));

        if (!Beneficiary.STATUS_PENDING_VERIFICATION.equals(beneficiary.getStatus())) {
            throw new IllegalArgumentException("Beneficiary is not pending verification. Current status: " + beneficiary.getStatus());
        }

        // Reject beneficiary - mark as blocked with reason in nickname temporarily (can add rejection_reason field later)
        beneficiary.setStatus(Beneficiary.STATUS_BLOCKED);
        beneficiary.setVerifiedBy(currentUser.getUsername());
        beneficiary.setVerifiedAt(LocalDateTime.now());

        beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary {} rejected by {} with reason: {}", beneficiaryId, currentUser.getUsername(), reason);
    }

    /**
     * Block beneficiary (Admin only)
     */
    @Transactional
    public void blockBeneficiary(Long beneficiaryId, String reason) {
        User currentUser = getCurrentUser();
        
        if (!currentUser.hasPermission("ACCOUNT_WRITE")) {
            throw new SecurityException("Access denied");
        }

        log.info("Admin {} blocking beneficiary: {}", currentUser.getUsername(), beneficiaryId);

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found: " + beneficiaryId));

        if (Beneficiary.STATUS_BLOCKED.equals(beneficiary.getStatus())) {
            throw new IllegalArgumentException("Beneficiary is already blocked");
        }

        beneficiary.setStatus(Beneficiary.STATUS_BLOCKED);
        beneficiaryRepository.save(beneficiary);
        
        log.info("Beneficiary {} blocked by {} with reason: {}", beneficiaryId, currentUser.getUsername(), reason);
    }

    // Helper methods

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found: " + username));
    }

    private Customer getCustomerForUser(User user) {
        if (user.getCustomer() == null) {
            throw new IllegalStateException("User is not associated with a customer account");
        }
        return user.getCustomer();
    }

    private BeneficiaryResponse mapToResponse(Beneficiary beneficiary) {
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .accountNumber(beneficiary.getAccountNumber())
                .ifscCode(beneficiary.getIfscCode())
                .bankName(beneficiary.getBankName())
                .branchName(beneficiary.getBranchName())
                .nickname(beneficiary.getNickname())
                .email(beneficiary.getEmail())
                .mobile(beneficiary.getMobile())
                .isVerified(beneficiary.getIsVerified())
                .status(beneficiary.getStatus())
                .lastUsedAt(beneficiary.getLastUsedAt())
                .createdAt(beneficiary.getCreatedAt())
                .updatedAt(beneficiary.getUpdatedAt())
                .build();
    }
}

