package com.javaweb.ai.service;

import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadActivity;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.lead.enums.LeadPriority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
class LeadScoreScorer {

    LeadScoreDraft score(
            Lead lead,
            List<LeadActivity> activities,
            List<FollowUpTask> tasks,
            List<Appointment> appointments,
            Instant now
    ) {
        List<String> reasons = new ArrayList<>();
        int score = 25;

        score += scoreStatus(lead.getStatus(), reasons);
        score += scoreSource(lead, reasons);
        score += scoreCustomer(lead.getCustomer(), reasons);
        score += scoreActivities(activities, reasons);
        score += scoreAppointments(appointments, reasons);
        score += scoreFollowUps(tasks, now, reasons);
        score += scoreRecency(lead, activities, now, reasons);
        score += scoreContactability(lead, reasons);

        int boundedScore = Math.max(0, Math.min(100, score));
        LeadPriority priority = priority(boundedScore);
        return new LeadScoreDraft(
                boundedScore,
                priority,
                String.join("; ", reasons),
                suggestedFollowUp(lead, tasks, priority)
        );
    }

    private int scoreStatus(LeadPipelineStatus status, List<String> reasons) {
        return switch (status) {
            case NEW -> {
                reasons.add("New lead needs first response");
                yield 10;
            }
            case ASSIGNED -> {
                reasons.add("Assigned lead is ready for outreach");
                yield 12;
            }
            case CONTACTED -> {
                reasons.add("Lead has already been contacted");
                yield 16;
            }
            case INTERESTED -> {
                reasons.add("Lead is marked interested");
                yield 24;
            }
            case VIEWING_SCHEDULED -> {
                reasons.add("Viewing is scheduled");
                yield 30;
            }
            case NEGOTIATING -> {
                reasons.add("Lead is in negotiation");
                yield 32;
            }
            case CLOSED_WON -> {
                reasons.add("Lead is already closed won");
                yield 0;
            }
            case CLOSED_LOST, INVALID -> {
                reasons.add("Lead is terminal and should not be prioritized");
                yield -40;
            }
        };
    }

    private int scoreSource(Lead lead, List<String> reasons) {
        if (lead.getSource() == null || lead.getSource().getCode() == null) {
            return 0;
        }
        return switch (lead.getSource().getCode()) {
            case "LISTING_INQUIRY" -> {
                reasons.add("Lead came from a listing inquiry");
                yield 12;
            }
            case "WEBSITE", "CHATBOT" -> {
                reasons.add("Lead came from an inbound digital channel");
                yield 9;
            }
            case "REFERRAL" -> {
                reasons.add("Lead came from a referral");
                yield 8;
            }
            default -> 3;
        };
    }

    private int scoreCustomer(Customer customer, List<String> reasons) {
        if (customer == null) {
            return 0;
        }
        int score = 0;
        List<CustomerRequirement> activeRequirements = customer.getRequirements().stream()
                .filter(CustomerRequirement::isActive)
                .toList();
        if (!activeRequirements.isEmpty()) {
            reasons.add("Customer has active buying or renting requirements");
            score += 8;
        }
        if (!customer.getFavoriteListings().isEmpty()) {
            reasons.add("Customer has favorite listings");
            score += 6;
        }
        return score;
    }

    private int scoreActivities(List<LeadActivity> activities, List<String> reasons) {
        if (activities.isEmpty()) {
            reasons.add("No interaction has been recorded yet");
            return -4;
        }
        int score = Math.min(18, activities.size() * 4);
        reasons.add(activities.size() + " interaction(s) recorded");
        return score;
    }

    private int scoreAppointments(List<Appointment> appointments, List<String> reasons) {
        if (appointments.isEmpty()) {
            return 0;
        }
        if (appointments.stream().anyMatch(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)) {
            reasons.add("Lead has completed a viewing appointment");
            return 20;
        }
        if (appointments.stream().anyMatch(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMED)) {
            reasons.add("Lead has a confirmed appointment");
            return 16;
        }
        if (appointments.stream().anyMatch(appointment -> appointment.getStatus() == AppointmentStatus.PENDING)) {
            reasons.add("Lead has a pending appointment");
            return 10;
        }
        return 0;
    }

    private int scoreFollowUps(
            List<FollowUpTask> tasks,
            Instant now,
            List<String> reasons
    ) {
        long overdue = tasks.stream()
                .filter(task -> task.getStatus() == FollowUpTaskStatus.PENDING
                        || task.getStatus() == FollowUpTaskStatus.IN_PROGRESS)
                .filter(task -> task.getDueAt().isBefore(now))
                .count();
        if (overdue > 0) {
            reasons.add(overdue + " follow-up task(s) are overdue");
            return -12;
        }
        boolean upcoming = tasks.stream()
                .filter(task -> task.getStatus() == FollowUpTaskStatus.PENDING
                        || task.getStatus() == FollowUpTaskStatus.IN_PROGRESS)
                .anyMatch(task -> task.getDueAt().isBefore(now.plus(Duration.ofDays(2))));
        if (upcoming) {
            reasons.add("Follow-up is due soon");
            return 6;
        }
        return 0;
    }

    private int scoreRecency(
            Lead lead,
            List<LeadActivity> activities,
            Instant now,
            List<String> reasons
    ) {
        Instant latest = lead.getLastContactedAt();
        if (latest == null) {
            latest = activities.stream()
                    .map(LeadActivity::getOccurredAt)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }
        if (latest == null) {
            return 0;
        }
        long days = Duration.between(latest, now).toDays();
        if (days <= 1) {
            reasons.add("Recent customer interaction");
            return 10;
        }
        if (days <= 7) {
            reasons.add("Customer interaction within the last week");
            return 5;
        }
        reasons.add("No recent customer interaction");
        return -8;
    }

    private int scoreContactability(Lead lead, List<String> reasons) {
        if (lead.getEmail() != null && lead.getPhone() != null) {
            reasons.add("Lead has both email and phone contact details");
            return 6;
        }
        if (lead.getEmail() != null || lead.getPhone() != null) {
            reasons.add("Lead has one contact channel");
            return 3;
        }
        reasons.add("Lead has no direct contact channel");
        return -10;
    }

    private LeadPriority priority(int score) {
        if (score >= 75) {
            return LeadPriority.HIGH;
        }
        if (score >= 45) {
            return LeadPriority.MEDIUM;
        }
        return LeadPriority.LOW;
    }

    private String suggestedFollowUp(
            Lead lead,
            List<FollowUpTask> tasks,
            LeadPriority priority
    ) {
        if (priority == LeadPriority.HIGH) {
            return "Call the lead today and confirm the next appointment or negotiation step";
        }
        boolean hasOpenTask = tasks.stream()
                .anyMatch(task -> task.getStatus() == FollowUpTaskStatus.PENDING
                        || task.getStatus() == FollowUpTaskStatus.IN_PROGRESS);
        if (!hasOpenTask) {
            return "Create a follow-up task and send suitable listings based on the lead context";
        }
        if (lead.getStatus() == LeadPipelineStatus.NEW || lead.getStatus() == LeadPipelineStatus.ASSIGNED) {
            return "Make first contact and record the customer's budget and location preferences";
        }
        return "Continue the planned follow-up and update the pipeline after the next interaction";
    }
}
