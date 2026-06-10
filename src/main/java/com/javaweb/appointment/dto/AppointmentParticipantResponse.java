package com.javaweb.appointment.dto;

import com.javaweb.appointment.enums.AppointmentParticipantRole;
import com.javaweb.appointment.enums.ParticipantResponseStatus;

import java.time.Instant;

public record AppointmentParticipantResponse(
        Long id,
        Long userId,
        String userName,
        AppointmentParticipantRole participantRole,
        ParticipantResponseStatus responseStatus,
        Instant respondedAt,
        String notes
) {
}
