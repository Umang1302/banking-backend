package com.nedbank.banking.repository;

import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.QRPaymentRequest;
import com.nedbank.banking.entity.QRTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QRTransactionRepository extends JpaRepository<QRTransaction, Long> {

    Optional<QRTransaction> findByTransactionReference(String transactionReference);

    Optional<QRTransaction> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<QRTransaction> findByRazorpayOrderId(String razorpayOrderId);

    List<QRTransaction> findByQrRequest(QRPaymentRequest qrRequest);

    List<QRTransaction> findByPayerAccountOrderByInitiatedAtDesc(Account payerAccount);

    List<QRTransaction> findByReceiverAccountOrderByInitiatedAtDesc(Account receiverAccount);

    List<QRTransaction> findByStatus(String status);

    List<QRTransaction> findByPaymentType(String paymentType);

    @Query("SELECT q FROM QRTransaction q WHERE q.payerAccount.id = :accountId ORDER BY q.initiatedAt DESC")
    List<QRTransaction> findByPayerAccountId(@Param("accountId") Long accountId);

    @Query("SELECT q FROM QRTransaction q WHERE q.receiverAccount.id = :accountId ORDER BY q.initiatedAt DESC")
    List<QRTransaction> findByReceiverAccountId(@Param("accountId") Long accountId);

    @Query("SELECT q FROM QRTransaction q WHERE (q.payerAccount.id = :accountId OR q.receiverAccount.id = :accountId) ORDER BY q.initiatedAt DESC")
    List<QRTransaction> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT q FROM QRTransaction q WHERE q.status = :status AND q.paymentType = :paymentType")
    List<QRTransaction> findByStatusAndPaymentType(@Param("status") String status, @Param("paymentType") String paymentType);

    @Query("SELECT q FROM QRTransaction q WHERE q.initiatedBy = :username ORDER BY q.initiatedAt DESC")
    List<QRTransaction> findByInitiatedBy(@Param("username") String username);

    @Query("SELECT q FROM QRTransaction q WHERE q.initiatedAt BETWEEN :startDate AND :endDate ORDER BY q.initiatedAt DESC")
    List<QRTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT q FROM QRTransaction q WHERE q.payerAccount.id = :accountId AND q.initiatedAt BETWEEN :startDate AND :endDate ORDER BY q.initiatedAt DESC")
    List<QRTransaction> findByPayerAccountIdAndDateRange(@Param("accountId") Long accountId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT q FROM QRTransaction q WHERE q.status IN :statuses")
    List<QRTransaction> findByStatusIn(@Param("statuses") List<String> statuses);

    boolean existsByRazorpayPaymentId(String razorpayPaymentId);

    boolean existsByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT COUNT(q) FROM QRTransaction q WHERE q.payerAccount.id = :accountId AND q.status = 'SETTLED'")
    long countSuccessfulTransactionsByPayerAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COUNT(q) FROM QRTransaction q WHERE q.receiverAccount.id = :accountId AND q.status = 'SETTLED'")
    long countSuccessfulTransactionsByReceiverAccountId(@Param("accountId") Long accountId);
}

