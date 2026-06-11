package com.javaweb.lead.repository;

import com.javaweb.lead.entity.LeadAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadAssignmentRepository extends JpaRepository<LeadAssignment, Long> {
    Optional<LeadAssignment> findFirstByLeadIdAndActiveTrueOrderByAssignedAtDesc(Long leadId);

    List<LeadAssignment> findAllByLeadIdOrderByAssignedAtDesc(Long leadId);
}
