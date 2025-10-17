package com.nedbank.banking.repository;

import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.QRPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QRPaymentRequestRepository extends JpaRepository<QRPaymentRequest, Long> {

    Optional<QRPaymentRequest> findByRequestId(String requestId);

    List<QRPaymentRequest> findByReceiverAccountOrderByCreatedAtDesc(Account receiverAccount);

    List<QRPaymentRequest> findByPayerAccountOrderByPaidAtDesc(Account payerAccount);

    List<QRPaymentRequest> findByStatus(String status);

    @Query("SELECT q FROM QRPaymentRequest q WHERE q.receiverAccount.id = :accountId ORDER BY q.createdAt DESC")
    List<QRPaymentRequest> findByReceiverAccountId(@Param("accountId") Long accountId);

    @Query("SELECT q FROM QRPaymentRequest q WHERE q.payerAccount.id = :accountId ORDER BY q.paidAt DESC")
    List<QRPaymentRequest> findByPayerAccountId(@Param("accountId") Long accountId);

    @Query("SELECT q FROM QRPaymentRequest q WHERE q.status = :status AND q.expiresAt < :now")
    List<QRPaymentRequest> findExpiredRequests(@Param("status") String status, @Param("now") LocalDateTime now);

    @Query("SELECT q FROM QRPaymentRequest q WHERE q.receiverAccount.customer.id = :customerId ORDER BY q.createdAt DESC")
    List<QRPaymentRequest> findByReceiverCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT q FROM QRPaymentRequest q WHERE q.createdBy = :username ORDER BY q.createdAt DESC")
    List<QRPaymentRequest> findByCreatedBy(@Param("username") String username);

    @Query("SELECT q FROM QRPaymentRequest q WHERE q.status = 'CREATED' AND q.expiresAt > :now")
    List<QRPaymentRequest> findActiveRequests(@Param("now") LocalDateTime now);

    boolean existsByRequestId(String requestId);
}

