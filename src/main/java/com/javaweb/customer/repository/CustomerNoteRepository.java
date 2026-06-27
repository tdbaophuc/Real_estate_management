package com.javaweb.customer.repository;

import com.javaweb.customer.entity.CustomerNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {
    Page<CustomerNote> findAllByCustomerIdOrderByPinnedDescCreatedAtDesc(
            Long customerId,
            Pageable pageable
    );

    List<CustomerNote> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<CustomerNote> findByIdAndCustomerId(Long id, Long customerId);
}
