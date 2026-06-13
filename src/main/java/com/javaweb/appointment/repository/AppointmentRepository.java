package com.javaweb.appointment.repository;

import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {
    Optional<Appointment> findByCode(String code);

    boolean existsByCode(String code);

    Page<Appointment> findAllByAgentIdOrderByStartAtDesc(
            Long agentId,
            Pageable pageable
    );

    Page<Appointment> findAllByCustomerIdOrderByStartAtDesc(
            Long customerId,
            Pageable pageable
    );

    Page<Appointment> findAllByCreatedByIdOrderByStartAtDesc(
            Long createdById,
            Pageable pageable
    );

    List<Appointment> findAllByLeadIdOrderByStartAtDesc(Long leadId);

    List<Appointment> findAllByReminderSentAtIsNullAndStatusInAndStartAtBetweenOrderByStartAtAsc(
            Collection<AppointmentStatus> statuses,
            Instant startAt,
            Instant endAt,
            Pageable pageable
    );

    @Query("""
            select count(appointment) > 0
            from Appointment appointment
            where appointment.agent.id = :agentId
              and appointment.status not in :excludedStatuses
              and appointment.startAt < :endAt
              and appointment.endAt > :startAt
            """)
    boolean existsAgentConflict(
            @Param("agentId") Long agentId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludedStatuses") Collection<AppointmentStatus> excludedStatuses
    );

    @Query("""
            select count(appointment) > 0
            from Appointment appointment
            where appointment.property.id = :propertyId
              and appointment.status not in :excludedStatuses
              and appointment.startAt < :endAt
              and appointment.endAt > :startAt
            """)
    boolean existsPropertyConflict(
            @Param("propertyId") Long propertyId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludedStatuses") Collection<AppointmentStatus> excludedStatuses
    );
}
