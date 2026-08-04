package com.javaweb.customer.repository;

import com.javaweb.customer.entity.CustomerRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRequirementRepository
        extends JpaRepository<CustomerRequirement, Long> {
    List<CustomerRequirement> findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(
            Long customerId
    );

    List<CustomerRequirement> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<CustomerRequirement> findByIdAndCustomerId(Long id, Long customerId);
}
