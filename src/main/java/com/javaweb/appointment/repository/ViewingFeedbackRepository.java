package com.javaweb.appointment.repository;

import com.javaweb.appointment.entity.ViewingFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ViewingFeedbackRepository extends JpaRepository<ViewingFeedback, Long> {
    List<ViewingFeedback> findAllByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    Optional<ViewingFeedback> findByAppointmentIdAndSubmittedById(
            Long appointmentId,
            Long submittedById
    );
}
