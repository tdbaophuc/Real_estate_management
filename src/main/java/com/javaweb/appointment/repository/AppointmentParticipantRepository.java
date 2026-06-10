package com.javaweb.appointment.repository;

import com.javaweb.appointment.entity.AppointmentParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentParticipantRepository
        extends JpaRepository<AppointmentParticipant, Long> {
    List<AppointmentParticipant> findAllByAppointmentIdOrderByCreatedAtAsc(
            Long appointmentId
    );

    boolean existsByAppointmentIdAndUserId(Long appointmentId, Long userId);
}
