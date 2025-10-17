package com.nedbank.banking.repository;

import com.nedbank.banking.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    /**
     * Find all beneficiaries for a customer
     */
    List<Beneficiary> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /**
     * Find active beneficiaries for a customer
     */
    List<Beneficiary> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, String status);

    /**
     * Find beneficiary by ID and customer (for security check)
     */
    Optional<Beneficiary> findByIdAndCustomerId(Long id, Long customerId);

    /**
     * Check if beneficiary already exists for customer
     */
    boolean existsByCustomerIdAndAccountNumberAndIfscCode(Long customerId, String accountNumber, String ifscCode);

    /**
     * Find beneficiary by account number and IFSC
     */
    Optional<Beneficiary> findByCustomerIdAndAccountNumberAndIfscCode(Long customerId, String accountNumber, String ifscCode);

    /**
     * Count active beneficiaries for a customer
     */
    long countByCustomerIdAndStatus(Long customerId, String status);

    /**
     * Find all beneficiaries (admin) - excludes inactive/deleted ones
     */
    @Query("SELECT b FROM Beneficiary b WHERE b.status <> 'INACTIVE' ORDER BY b.createdAt DESC")
    List<Beneficiary> findAllBeneficiaries();

    /**
     * Find beneficiaries by status (admin)
     */
    List<Beneficiary> findByStatusOrderByCreatedAtAsc(String status);
}

