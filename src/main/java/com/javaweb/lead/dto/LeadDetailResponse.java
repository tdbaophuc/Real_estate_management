package com.javaweb.lead.dto;

import java.util.List;

public record LeadDetailResponse(
        LeadResponse lead,
        List<LeadAssignmentResponse> assignments,
        List<LeadNoteResponse> notes,
        List<LeadActivityResponse> activities,
        List<FollowUpTaskResponse> followUpTasks
) {
}
