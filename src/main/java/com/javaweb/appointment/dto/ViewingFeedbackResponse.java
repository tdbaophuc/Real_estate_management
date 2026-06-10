package com.javaweb.appointment.dto;

import com.javaweb.appointment.enums.ViewingInterestLevel;

import java.time.Instant;

public record ViewingFeedbackResponse(
        Long id,
        Long appointmentId,
        Long submittedById,
        String submittedByName,
        Integer rating,
        ViewingInterestLevel interestLevel,
        String comments,
        String positivePoints,
        String concerns,
        String nextAction,
        Instant createdAt
) {
}
