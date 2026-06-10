package com.javaweb.lead.mapper;

import com.javaweb.auth.entity.User;
import com.javaweb.customer.entity.Customer;
import com.javaweb.lead.dto.FollowUpTaskResponse;
import com.javaweb.lead.dto.LeadActivityResponse;
import com.javaweb.lead.dto.LeadAssignmentResponse;
import com.javaweb.lead.dto.LeadNoteResponse;
import com.javaweb.lead.dto.LeadResponse;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadActivity;
import com.javaweb.lead.entity.LeadAssignment;
import com.javaweb.lead.entity.LeadNote;
import com.javaweb.listing.entity.Listing;
import org.springframework.stereotype.Component;

@Component
public class LeadMapper {

    public LeadResponse toResponse(Lead lead) {
        Customer customer = lead.getCustomer();
        Listing listing = lead.getListing();
        User assignee = lead.getCurrentAssignee();
        User creator = lead.getCreatedBy();
        return new LeadResponse(
                lead.getId(),
                lead.getCode(),
                lead.getFullName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getStatus(),
                lead.getPriority(),
                lead.getScore(),
                lead.getMessage(),
                lead.getLostReason(),
                lead.getSource().getId(),
                lead.getSource().getCode(),
                lead.getSource().getName(),
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getFullName(),
                listing == null ? null : listing.getId(),
                listing == null ? null : listing.getTitle(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getFullName(),
                creator == null ? null : creator.getId(),
                creator == null ? null : creator.getFullName(),
                lead.getLastContactedAt(),
                lead.getConvertedAt(),
                lead.getClosedAt(),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }

    public LeadAssignmentResponse toAssignmentResponse(LeadAssignment assignment) {
        User assignedBy = assignment.getAssignedBy();
        return new LeadAssignmentResponse(
                assignment.getId(),
                assignment.getLead().getId(),
                assignment.getAssignedTo().getId(),
                assignment.getAssignedTo().getFullName(),
                assignedBy == null ? null : assignedBy.getId(),
                assignedBy == null ? null : assignedBy.getFullName(),
                assignment.getAssignedAt(),
                assignment.getUnassignedAt(),
                assignment.isActive(),
                assignment.getNotes()
        );
    }

    public LeadNoteResponse toNoteResponse(LeadNote note) {
        return new LeadNoteResponse(
                note.getId(),
                note.getLead().getId(),
                note.getAuthor().getId(),
                note.getAuthor().getFullName(),
                note.getContent(),
                note.isPinned(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    public LeadActivityResponse toActivityResponse(LeadActivity activity) {
        User actor = activity.getActor();
        return new LeadActivityResponse(
                activity.getId(),
                activity.getLead().getId(),
                activity.getActivityType(),
                activity.getSubject(),
                activity.getDetails(),
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getFullName(),
                activity.getOccurredAt(),
                activity.getCreatedAt()
        );
    }

    public FollowUpTaskResponse toTaskResponse(FollowUpTask task) {
        return new FollowUpTaskResponse(
                task.getId(),
                task.getLead().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssignedTo().getId(),
                task.getAssignedTo().getFullName(),
                task.getCreatedBy().getId(),
                task.getCreatedBy().getFullName(),
                task.getDueAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
