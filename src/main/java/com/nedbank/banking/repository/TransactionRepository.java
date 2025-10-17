package com.nedbank.banking.repository;

import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    List<Transaction> findByAccount(Account account);

    List<Transaction> findByAccountId(Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountIdOrderByTransactionDateDesc(@Param("accountId") Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.status = :status ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountIdAndStatus(@Param("accountId") Long accountId, @Param("status") String status);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountIdAndDateRange(@Param("accountId") Long accountId, 
                                                   @Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.account.customer.id = :customerId ORDER BY t.transactionDate DESC")
    List<Transaction> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT t FROM Transaction t WHERE t.transactionType = :transactionType ORDER BY t.transactionDate DESC")
    List<Transaction> findByTransactionType(@Param("transactionType") String transactionType);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status ORDER BY t.transactionDate DESC")
    List<Transaction> findByStatus(@Param("status") String status);

    @Query("SELECT t FROM Transaction t WHERE t.bulkUploadBatchId = :batchId ORDER BY t.transactionDate DESC")
    List<Transaction> findByBulkUploadBatchId(@Param("batchId") String batchId);

    @Query("SELECT t FROM Transaction t WHERE t.initiatedBy = :username ORDER BY t.transactionDate DESC")
    List<Transaction> findByInitiatedBy(@Param("username") String username);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.id = :accountId")
    long countByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT t FROM Transaction t WHERE t.transactionDate >= :date ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactions(@Param("date") LocalDateTime date);

    @Query("SELECT t FROM Transaction t WHERE t.account.customer.id = :customerId ORDER BY t.transactionDate DESC")
    List<Transaction> findTop10ByCustomerIdOrderByTransactionDateDesc(@Param("customerId") Long customerId);

    boolean existsByTransactionReference(String transactionReference);
}

