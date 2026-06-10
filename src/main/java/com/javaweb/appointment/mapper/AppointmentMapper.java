package com.javaweb.appointment.mapper;

import com.javaweb.appointment.dto.AppointmentParticipantResponse;
import com.javaweb.appointment.dto.AppointmentResponse;
import com.javaweb.appointment.dto.ViewingFeedbackResponse;
import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.entity.AppointmentParticipant;
import com.javaweb.appointment.entity.ViewingFeedback;
import com.javaweb.auth.entity.User;
import com.javaweb.lead.entity.Lead;
import com.javaweb.listing.entity.Listing;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment appointment) {
        Listing listing = appointment.getListing();
        Lead lead = appointment.getLead();
        User cancelledBy = appointment.getCancelledBy();
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getCode(),
                appointment.getTitle(),
                appointment.getStatus(),
                appointment.getCustomer().getId(),
                appointment.getCustomer().getFullName(),
                appointment.getAgent().getId(),
                appointment.getAgent().getFullName(),
                appointment.getProperty().getId(),
                appointment.getProperty().getName(),
                listing == null ? null : listing.getId(),
                listing == null ? null : listing.getTitle(),
                lead == null ? null : lead.getId(),
                lead == null ? null : lead.getCode(),
                appointment.getCreatedBy().getId(),
                appointment.getCreatedBy().getFullName(),
                appointment.getRescheduledFrom() == null
                        ? null
                        : appointment.getRescheduledFrom().getId(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getTimezone(),
                appointment.getMeetingLocation(),
                appointment.getNotes(),
                appointment.getCancellationReason(),
                cancelledBy == null ? null : cancelledBy.getId(),
                appointment.getConfirmedAt(),
                appointment.getCancelledAt(),
                appointment.getCompletedAt(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt(),
                appointment.getParticipants().stream()
                        .map(this::toParticipantResponse)
                        .toList(),
                appointment.getFeedbacks().stream()
                        .map(this::toFeedbackResponse)
                        .toList()
        );
    }

    public ViewingFeedbackResponse toFeedbackResponse(ViewingFeedback feedback) {
        return new ViewingFeedbackResponse(
                feedback.getId(),
                feedback.getAppointment().getId(),
                feedback.getSubmittedBy().getId(),
                feedback.getSubmittedBy().getFullName(),
                feedback.getRating(),
                feedback.getInterestLevel(),
                feedback.getComments(),
                feedback.getPositivePoints(),
                feedback.getConcerns(),
                feedback.getNextAction(),
                feedback.getCreatedAt()
        );
    }

    private AppointmentParticipantResponse toParticipantResponse(
            AppointmentParticipant participant
    ) {
        return new AppointmentParticipantResponse(
                participant.getId(),
                participant.getUser().getId(),
                participant.getUser().getFullName(),
                participant.getParticipantRole(),
                participant.getResponseStatus(),
                participant.getRespondedAt(),
                participant.getNotes()
        );
    }
}
