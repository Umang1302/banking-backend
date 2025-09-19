package com.nedbank.banking.repository;

import com.nedbank.banking.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByMobile(String mobile);

    Optional<Customer> findByNationalId(String nationalId);

    @Query("SELECT c FROM Customer c WHERE c.status = :status")
    List<Customer> findByStatus(@Param("status") String status);

    @Query("SELECT c FROM Customer c WHERE c.firstName LIKE %:name% OR c.lastName LIKE %:name%")
    List<Customer> findByNameContaining(@Param("name") String name);

    boolean existsByCustomerNumber(String customerNumber);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    boolean existsByNationalId(String nationalId);
}
