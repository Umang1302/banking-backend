package com.nedbank.banking.repository;

import com.nedbank.banking.entity.Account;
import com.nedbank.banking.entity.UPIAccount;
import com.nedbank.banking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UPIAccountRepository extends JpaRepository<UPIAccount, Long> {

    Optional<UPIAccount> findByUpiId(String upiId);

    List<UPIAccount> findByUser(User user);

    List<UPIAccount> findByAccount(Account account);

    @Query("SELECT u FROM UPIAccount u WHERE u.user.id = :userId ORDER BY u.isPrimary DESC, u.createdAt DESC")
    List<UPIAccount> findByUserId(@Param("userId") Long userId);

    @Query("SELECT u FROM UPIAccount u WHERE u.account.id = :accountId AND u.status = 'ACTIVE'")
    List<UPIAccount> findActiveByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT u FROM UPIAccount u WHERE u.user.id = :userId AND u.isPrimary = true AND u.status = 'ACTIVE'")
    Optional<UPIAccount> findPrimaryByUserId(@Param("userId") Long userId);

    @Query("SELECT u FROM UPIAccount u WHERE u.upiId = :upiId AND u.status = 'ACTIVE'")
    Optional<UPIAccount> findActiveByUpiId(@Param("upiId") String upiId);

    List<UPIAccount> findByStatus(String status);

    @Query("SELECT u FROM UPIAccount u WHERE u.user.username = :username ORDER BY u.isPrimary DESC, u.createdAt DESC")
    List<UPIAccount> findByUsername(@Param("username") String username);

    boolean existsByUpiId(String upiId);

    @Query("SELECT COUNT(u) FROM UPIAccount u WHERE u.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(u) FROM UPIAccount u WHERE u.account.id = :accountId")
    long countByAccountId(@Param("accountId") Long accountId);
}

