package com.javaweb.lead.service;

import com.javaweb.audit.AuditActions;
import com.javaweb.audit.service.AuditLogService;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import com.javaweb.lead.dto.FollowUpTaskResponse;
import com.javaweb.lead.dto.FollowUpTaskSearchRequest;
import com.javaweb.lead.dto.FollowUpTaskStatusRequest;
import com.javaweb.lead.dto.FollowUpTaskUpdateRequest;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.mapper.LeadMapper;
import com.javaweb.lead.repository.FollowUpTaskRepository;
import com.javaweb.lead.repository.FollowUpTaskSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FollowUpTaskService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "title", "title",
            "status", "status",
            "priority", "priority",
            "dueAt", "dueAt",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private final FollowUpTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final LeadMapper leadMapper;
    private final AuditLogService auditLogService;

    public FollowUpTaskService(
            FollowUpTaskRepository taskRepository,
            UserRepository userRepository,
            LeadMapper leadMapper,
            AuditLogService auditLogService
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.leadMapper = leadMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUpTaskResponse> search(
            FollowUpTaskSearchRequest request,
            AuthUserPrincipal actor
    ) {
        String sortField = requireSortField(request.sortBy());
        Long visibleUserId = isManagerOrAdmin(actor) ? null : actor.id();
        Page<FollowUpTask> page = taskRepository.findAll(
                FollowUpTaskSpecifications.search(request, visibleUserId),
                PageRequest.of(
                        request.page(),
                        request.size(),
                        Sort.by(request.sortDirection(), sortField)
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                )
        );
        return PageResponse.from(
                page,
                page.getContent().stream().map(leadMapper::toTaskResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUpTaskResponse> searchMine(
            FollowUpTaskSearchRequest request,
            AuthUserPrincipal actor
    ) {
        FollowUpTaskSearchRequest mineRequest = new FollowUpTaskSearchRequest(
                request.status(),
                request.priority(),
                request.leadId(),
                actor.id(),
                request.dueFrom(),
                request.dueTo(),
                request.keyword(),
                request.page(),
                request.size(),
                request.sortBy(),
                request.sortDirection()
        );
        return search(mineRequest, actor);
    }

    @Transactional(readOnly = true)
    public FollowUpTaskResponse get(Long taskId, AuthUserPrincipal actor) {
        return leadMapper.toTaskResponse(requireAccessibleTask(taskId, actor));
    }

    @Transactional
    public FollowUpTaskResponse update(
            Long taskId,
            FollowUpTaskUpdateRequest request,
            AuthUserPrincipal actor
    ) {
        FollowUpTask task = requireAccessibleTask(taskId, actor);
        if (task.getStatus() == FollowUpTaskStatus.CANCELLED) {
            throw new BusinessException("Cancelled follow-up tasks cannot be updated");
        }
        Map<String, Object> oldValue = taskAuditValue(task);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueAt(request.dueAt());
        if (request.assignedAgentId() != null) {
            task.setAssignedTo(resolveTaskAssignee(request.assignedAgentId(), actor));
        }
        FollowUpTask saved = taskRepository.saveAndFlush(task);
        auditLogService.record(
                actor,
                AuditActions.FOLLOW_UP_TASK_UPDATED,
                AuditActions.FOLLOW_UP_TASK,
                saved.getId(),
                oldValue,
                taskAuditValue(saved)
        );
        return leadMapper.toTaskResponse(saved);
    }

    @Transactional
    public FollowUpTaskResponse updateStatus(
            Long taskId,
            FollowUpTaskStatusRequest request,
            AuthUserPrincipal actor
    ) {
        FollowUpTask task = requireAccessibleTask(taskId, actor);
        FollowUpTaskStatus previousStatus = task.getStatus();
        if (previousStatus == request.status()) {
            throw new BusinessException("Follow-up task already has the requested status");
        }
        Map<String, Object> oldValue = taskAuditValue(task);
        applyStatus(task, request);
        FollowUpTask saved = taskRepository.saveAndFlush(task);
        auditLogService.record(
                actor,
                AuditActions.FOLLOW_UP_TASK_STATUS_CHANGED,
                AuditActions.FOLLOW_UP_TASK,
                saved.getId(),
                oldValue,
                taskAuditValue(saved)
        );
        return leadMapper.toTaskResponse(saved);
    }

    @Transactional
    public void cancel(Long taskId, AuthUserPrincipal actor) {
        FollowUpTask task = requireAccessibleTask(taskId, actor);
        if (task.getStatus() == FollowUpTaskStatus.CANCELLED) {
            return;
        }
        Map<String, Object> oldValue = taskAuditValue(task);
        task.setStatus(FollowUpTaskStatus.CANCELLED);
        task.setCompletedAt(null);
        taskRepository.saveAndFlush(task);
        auditLogService.record(
                actor,
                AuditActions.FOLLOW_UP_TASK_CANCELLED,
                AuditActions.FOLLOW_UP_TASK,
                task.getId(),
                oldValue,
                taskAuditValue(task)
        );
    }

    private FollowUpTask requireAccessibleTask(Long taskId, AuthUserPrincipal actor) {
        FollowUpTask task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up task not found"));
        if (task.getLead().getDeletedAt() != null) {
            throw new ResourceNotFoundException("Follow-up task not found");
        }
        if (!isManagerOrAdmin(actor) && !canAgentAccess(task, actor.id())) {
            throw new AccessDeniedException(
                    "Agents can only access follow-up tasks they own or are assigned"
            );
        }
        return task;
    }

    private boolean canAgentAccess(FollowUpTask task, Long actorId) {
        return task.getAssignedTo().getId().equals(actorId)
                || task.getCreatedBy().getId().equals(actorId)
                || (task.getLead().getCurrentAssignee() != null
                        && task.getLead().getCurrentAssignee().getId().equals(actorId))
                || (task.getLead().getCreatedBy() != null
                        && task.getLead().getCreatedBy().getId().equals(actorId));
    }

    private void applyStatus(FollowUpTask task, FollowUpTaskStatusRequest request) {
        task.setStatus(request.status());
        if (request.status() == FollowUpTaskStatus.COMPLETED) {
            task.setCompletedAt(request.completedAt() == null
                    ? Instant.now()
                    : request.completedAt());
        } else {
            task.setCompletedAt(null);
        }
    }

    private User resolveTaskAssignee(Long assignedAgentId, AuthUserPrincipal actor) {
        if (!isManagerOrAdmin(actor) && !assignedAgentId.equals(actor.id())) {
            throw new AccessDeniedException("Agents cannot assign tasks to another agent");
        }
        return requireActiveAgent(assignedAgentId);
    }

    private User requireActiveAgent(Long userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
        boolean agent = user.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(RoleCode.AGENT::equals);
        if (!agent) {
            throw new BusinessException("Assigned user must have the AGENT role");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Assigned agent must be active");
        }
        return user;
    }

    private String requireSortField(String sortBy) {
        String sortField = SORT_FIELDS.get(sortBy);
        if (sortField == null) {
            throw new BusinessException("Unsupported follow-up task sort field");
        }
        return sortField;
    }

    private Map<String, Object> taskAuditValue(FollowUpTask task) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("leadId", task.getLead().getId());
        value.put("title", task.getTitle());
        value.put("description", task.getDescription());
        value.put("status", task.getStatus().name());
        value.put("priority", task.getPriority().name());
        value.put("assignedToId", task.getAssignedTo().getId());
        value.put("dueAt", task.getDueAt());
        value.put("completedAt", task.getCompletedAt());
        return value;
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }
}
