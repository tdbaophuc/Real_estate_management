package com.javaweb.lead.repository;

import com.javaweb.lead.entity.LeadActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {
    Page<LeadActivity> findAllByLeadIdOrderByOccurredAtDesc(
            Long leadId,
            Pageable pageable
    );
}
