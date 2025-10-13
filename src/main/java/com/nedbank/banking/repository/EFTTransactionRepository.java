package com.nedbank.banking.repository;

import com.nedbank.banking.entity.EFTTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EFTTransactionRepository extends JpaRepository<EFTTransaction, Long> {

    /**
     * Find EFT transaction by reference
     */
    Optional<EFTTransaction> findByEftReference(String eftReference);

    /**
     * Find all EFT transactions for a source account
     */
    @Query("SELECT e FROM EFTTransaction e WHERE e.sourceAccount.id = :accountId ORDER BY e.createdAt DESC")
    List<EFTTransaction> findBySourceAccountIdOrderByCreatedAtDesc(@Param("accountId") Long accountId);

    /**
     * Find EFT transactions by status
     */
    List<EFTTransaction> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find EFT transactions by type and status
     */
    List<EFTTransaction> findByEftTypeAndStatusOrderByCreatedAtDesc(String eftType, String status);

    /**
     * Find EFT transactions by type only (all statuses)
     */
    List<EFTTransaction> findByEftTypeOrderByCreatedAtDesc(String eftType);

    /**
     * Find pending NEFT transactions for batch processing
     */
    @Query("SELECT e FROM EFTTransaction e WHERE e.eftType = 'NEFT' AND e.status IN ('PENDING', 'QUEUED') ORDER BY e.createdAt ASC")
    List<EFTTransaction> findPendingNEFTTransactions();

    /**
     * Find NEFT transactions by batch ID
     */
    List<EFTTransaction> findByBatchIdOrderByCreatedAtAsc(String batchId);

    /**
     * Find EFT transactions by date range
     */
    @Query("SELECT e FROM EFTTransaction e WHERE e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    List<EFTTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count transactions by status
     */
    long countByStatus(String status);

    /**
     * Count transactions by type and status
     */
    long countByEftTypeAndStatus(String eftType, String status);

    /**
     * Count transactions by type only (all statuses)
     */
    long countByEftType(String eftType);

    /**
     * Find all EFT transactions (admin)
     */
    @Query("SELECT e FROM EFTTransaction e ORDER BY e.createdAt DESC")
    List<EFTTransaction> findAllEFTTransactions();

    /**
     * Find recent batch IDs for NEFT
     */
    @Query("SELECT DISTINCT e.batchId FROM EFTTransaction e WHERE e.eftType = 'NEFT' AND e.batchId IS NOT NULL ORDER BY e.batchId DESC")
    List<String> findDistinctBatchIds();
}

