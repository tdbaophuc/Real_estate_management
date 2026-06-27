package com.javaweb.lead.dto;

import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.enums.LeadPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;

import java.time.Instant;

public record FollowUpTaskSearchRequest(
        FollowUpTaskStatus status,
        LeadPriority priority,
        @Positive(message = "leadId must be positive")
        Long leadId,
        @Positive(message = "assignedAgentId must be positive")
        Long assignedAgentId,
        Instant dueFrom,
        Instant dueTo,
        @Size(max = 200, message = "keyword must not exceed 200 characters")
        String keyword,
        @Min(value = 0, message = "page must be at least 0")
        Integer page,
        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size,
        String sortBy,
        Sort.Direction sortDirection
) {
    public FollowUpTaskSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "dueAt" : sortBy.trim();
        sortDirection = sortDirection == null ? Sort.Direction.ASC : sortDirection;
    }

    @AssertTrue(message = "dueFrom must not be after dueTo")
    public boolean isDueRangeValid() {
        return dueFrom == null || dueTo == null || !dueFrom.isAfter(dueTo);
    }
}
