package com.javaweb.customer.repository;

import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findByCodeAndDeletedAtIsNull(String code);

    Optional<Customer> findByUserIdAndDeletedAtIsNull(Long userId);

    boolean existsByCode(String code);

    boolean existsByUserId(Long userId);

    Page<Customer> findAllByStatusAndDeletedAtIsNull(
            CustomerStatus status,
            Pageable pageable
    );
}
