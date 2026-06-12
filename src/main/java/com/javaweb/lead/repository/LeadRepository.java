package com.javaweb.lead.repository;

import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.enums.LeadPipelineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LeadRepository
        extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
    Optional<Lead> findByCodeAndDeletedAtIsNull(String code);

    @EntityGraph(attributePaths = {
            "source",
            "customer",
            "listing",
            "listing.property",
            "currentAssignee",
            "createdBy"
    })
    Optional<Lead> findWithAiScoreDetailsById(Long id);

    boolean existsByCode(String code);

    Page<Lead> findAllByStatusAndDeletedAtIsNull(
            LeadPipelineStatus status,
            Pageable pageable
    );

    Page<Lead> findAllByCurrentAssigneeIdAndDeletedAtIsNull(
            Long currentAssigneeId,
            Pageable pageable
    );
}
