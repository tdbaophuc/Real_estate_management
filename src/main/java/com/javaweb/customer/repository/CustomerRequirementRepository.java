package com.javaweb.customer.repository;

import com.javaweb.customer.entity.CustomerRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRequirementRepository
        extends JpaRepository<CustomerRequirement, Long> {
    List<CustomerRequirement> findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(
            Long customerId
    );

    List<CustomerRequirement> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
