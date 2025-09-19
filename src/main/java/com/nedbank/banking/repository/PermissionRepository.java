package com.nedbank.banking.repository;

import com.nedbank.banking.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    @Query("SELECT p FROM Permission p WHERE p.name IN :names")
    List<Permission> findByNameIn(@Param("names") List<String> names);

    boolean existsByName(String name);

    @Query("SELECT p FROM Permission p ORDER BY p.name")
    List<Permission> findAllOrderByName();
}
