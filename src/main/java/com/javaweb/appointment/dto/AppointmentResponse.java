package com.javaweb.appointment.dto;

import com.javaweb.appointment.enums.AppointmentStatus;

import java.time.Instant;
import java.util.List;

public record AppointmentResponse(
        Long id,
        String code,
        String title,
        AppointmentStatus status,
        Long customerId,
        String customerName,
        Long agentId,
        String agentName,
        Long propertyId,
        String propertyName,
        Long listingId,
        String listingTitle,
        Long leadId,
        String leadCode,
        Long createdById,
        String createdByName,
        Long rescheduledFromId,
        Instant startAt,
        Instant endAt,
        String timezone,
        String meetingLocation,
        String notes,
        String cancellationReason,
        Long cancelledById,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        List<AppointmentParticipantResponse> participants,
        List<ViewingFeedbackResponse> feedbacks
) {
}
