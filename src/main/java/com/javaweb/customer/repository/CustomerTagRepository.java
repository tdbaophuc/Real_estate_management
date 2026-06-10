package com.javaweb.customer.repository;

import com.javaweb.customer.entity.CustomerTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerTagRepository extends JpaRepository<CustomerTag, Long> {
    boolean existsByCustomerIdAndNameIgnoreCase(Long customerId, String name);

    List<CustomerTag> findAllByCustomerIdOrderByName(Long customerId);
}
