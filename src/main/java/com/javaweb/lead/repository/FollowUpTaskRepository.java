package com.javaweb.lead.repository;

import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface FollowUpTaskRepository extends JpaRepository<FollowUpTask, Long> {
    Page<FollowUpTask> findAllByLeadIdOrderByDueAtAsc(Long leadId, Pageable pageable);

    List<FollowUpTask> findAllByLeadIdOrderByDueAtAsc(Long leadId);

    Page<FollowUpTask> findAllByAssignedToIdAndStatusInAndDueAtBeforeOrderByDueAtAsc(
            Long assignedToId,
            Collection<FollowUpTaskStatus> statuses,
            Instant dueAt,
            Pageable pageable
    );
}
