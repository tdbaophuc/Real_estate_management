package com.javaweb.lead.repository;

import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface FollowUpTaskRepository
        extends JpaRepository<FollowUpTask, Long>, JpaSpecificationExecutor<FollowUpTask> {
    @EntityGraph(attributePaths = {
            "lead",
            "lead.currentAssignee",
            "lead.createdBy",
            "assignedTo",
            "createdBy"
    })
    @Query("select task from FollowUpTask task where task.id = :id")
    java.util.Optional<FollowUpTask> findWithDetailsById(@Param("id") Long id);

    Page<FollowUpTask> findAllByLeadIdOrderByDueAtAsc(Long leadId, Pageable pageable);

    List<FollowUpTask> findAllByLeadIdOrderByDueAtAsc(Long leadId);

    Page<FollowUpTask> findAllByAssignedToIdAndStatusInAndDueAtBeforeOrderByDueAtAsc(
            Long assignedToId,
            Collection<FollowUpTaskStatus> statuses,
            Instant dueAt,
            Pageable pageable
    );

    List<FollowUpTask> findAllByReminderSentAtIsNullAndStatusInAndDueAtBetweenOrderByDueAtAsc(
            Collection<FollowUpTaskStatus> statuses,
            Instant startAt,
            Instant endAt,
            Pageable pageable
    );
}
