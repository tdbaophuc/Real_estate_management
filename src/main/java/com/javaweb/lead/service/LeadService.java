package com.javaweb.lead.service;

import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.DuplicateResourceException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.lead.dto.FollowUpTaskRequest;
import com.javaweb.lead.dto.FollowUpTaskResponse;
import com.javaweb.lead.dto.LeadActivityRequest;
import com.javaweb.lead.dto.LeadActivityResponse;
import com.javaweb.lead.dto.LeadAssignRequest;
import com.javaweb.lead.dto.LeadAssignmentResponse;
import com.javaweb.lead.dto.LeadCreateRequest;
import com.javaweb.lead.dto.LeadDetailResponse;
import com.javaweb.lead.dto.LeadNoteRequest;
import com.javaweb.lead.dto.LeadNoteResponse;
import com.javaweb.lead.dto.LeadResponse;
import com.javaweb.lead.dto.LeadSearchRequest;
import com.javaweb.lead.dto.LeadStatusUpdateRequest;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadActivity;
import com.javaweb.lead.entity.LeadAssignment;
import com.javaweb.lead.entity.LeadNote;
import com.javaweb.lead.entity.LeadSource;
import com.javaweb.lead.enums.LeadActivityType;
import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.lead.mapper.LeadMapper;
import com.javaweb.lead.repository.FollowUpTaskRepository;
import com.javaweb.lead.repository.LeadActivityRepository;
import com.javaweb.lead.repository.LeadAssignmentRepository;
import com.javaweb.lead.repository.LeadNoteRepository;
import com.javaweb.lead.repository.LeadRepository;
import com.javaweb.lead.repository.LeadSourceRepository;
import com.javaweb.lead.repository.LeadSpecifications;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.repository.ListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
public class LeadService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "code", "code",
            "fullName", "fullName",
            "status", "status",
            "priority", "priority",
            "score", "score",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );
    private static final Map<LeadPipelineStatus, Set<LeadPipelineStatus>>
            ALLOWED_TRANSITIONS = buildTransitions();
    private static final Set<LeadPipelineStatus> TERMINAL_STATUSES = Set.of(
            LeadPipelineStatus.CLOSED_WON,
            LeadPipelineStatus.CLOSED_LOST,
            LeadPipelineStatus.INVALID
    );

    private final LeadRepository leadRepository;
    private final LeadSourceRepository sourceRepository;
    private final LeadAssignmentRepository assignmentRepository;
    private final LeadNoteRepository noteRepository;
    private final LeadActivityRepository activityRepository;
    private final FollowUpTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ListingRepository listingRepository;
    private final LeadMapper leadMapper;

    public LeadService(
            LeadRepository leadRepository,
            LeadSourceRepository sourceRepository,
            LeadAssignmentRepository assignmentRepository,
            LeadNoteRepository noteRepository,
            LeadActivityRepository activityRepository,
            FollowUpTaskRepository taskRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            ListingRepository listingRepository,
            LeadMapper leadMapper
    ) {
        this.leadRepository = leadRepository;
        this.sourceRepository = sourceRepository;
        this.assignmentRepository = assignmentRepository;
        this.noteRepository = noteRepository;
        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.listingRepository = listingRepository;
        this.leadMapper = leadMapper;
    }

    @Transactional
    public LeadResponse create(LeadCreateRequest request, AuthUserPrincipal actor) {
        if (leadRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Lead code already exists");
        }
        User creator = requireUser(actor.id(), "Authenticated user not found");
        LeadSource source = sourceRepository.findByCodeAndActiveTrue(request.sourceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Active lead source not found"));
        Customer customer = resolveCustomer(request.customerId(), actor);
        Listing listing = resolveListing(request.listingId());
        User assignee = resolveCreateAssignee(request.assignedAgentId(), actor);

        Lead lead = new Lead(request.code(), source, request.fullName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setPriority(request.priority());
        lead.setMessage(request.message());
        lead.setCustomer(customer);
        lead.setListing(listing);
        lead.setCreatedBy(creator);
        if (assignee != null) {
            lead.setCurrentAssignee(assignee);
            lead.setStatus(LeadPipelineStatus.ASSIGNED);
            lead.addAssignment(new LeadAssignment(assignee, creator));
            LeadActivity activity = new LeadActivity(LeadActivityType.ASSIGNMENT, creator);
            activity.setSubject("Lead assigned");
            activity.setDetails("Assigned to " + assignee.getFullName());
            lead.addActivity(activity);
        }
        return leadMapper.toResponse(leadRepository.saveAndFlush(lead));
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> search(
            LeadSearchRequest request,
            AuthUserPrincipal actor
    ) {
        String sortField = SORT_FIELDS.get(request.sortBy());
        if (sortField == null) {
            throw new BusinessException("Unsupported lead sort field");
        }
        Long visibleUserId = isManagerOrAdmin(actor) ? null : actor.id();
        Page<Lead> page = leadRepository.findAll(
                LeadSpecifications.search(request, visibleUserId),
                PageRequest.of(
                        request.page(),
                        request.size(),
                        Sort.by(request.direction(), sortField)
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                )
        );
        return PageResponse.from(
                page,
                page.getContent().stream().map(leadMapper::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public LeadDetailResponse get(Long leadId, AuthUserPrincipal actor) {
        Lead lead = requireAccessibleLead(leadId, actor);
        return new LeadDetailResponse(
                leadMapper.toResponse(lead),
                assignmentRepository.findAllByLeadIdOrderByAssignedAtDesc(leadId)
                        .stream().map(leadMapper::toAssignmentResponse).toList(),
                noteRepository.findAllByLeadIdOrderByPinnedDescCreatedAtDesc(leadId)
                        .stream().map(leadMapper::toNoteResponse).toList(),
                activityRepository.findAllByLeadIdOrderByOccurredAtDesc(leadId)
                        .stream().map(leadMapper::toActivityResponse).toList(),
                taskRepository.findAllByLeadIdOrderByDueAtAsc(leadId)
                        .stream().map(leadMapper::toTaskResponse).toList()
        );
    }

    @Transactional
    public LeadAssignmentResponse assign(
            Long leadId,
            LeadAssignRequest request,
            AuthUserPrincipal actor
    ) {
        requireManagerOrAdmin(actor, "Only managers or administrators can assign leads");
        Lead lead = requireAccessibleLead(leadId, actor);
        requireOpenLead(lead);
        User assignee = requireActiveAgent(request.agentId());
        if (lead.getCurrentAssignee() != null
                && lead.getCurrentAssignee().getId().equals(assignee.getId())) {
            throw new BusinessException("Lead is already assigned to this agent");
        }
        User assignedBy = requireUser(actor.id(), "Authenticated user not found");
        Instant now = Instant.now();
        assignmentRepository
                .findFirstByLeadIdAndActiveTrueOrderByAssignedAtDesc(leadId)
                .ifPresent(current -> {
                    current.setActive(false);
                    current.setUnassignedAt(now);
                });

        LeadAssignment assignment = new LeadAssignment(assignee, assignedBy);
        assignment.setNotes(request.notes());
        lead.addAssignment(assignment);
        lead.setCurrentAssignee(assignee);
        if (lead.getStatus() == LeadPipelineStatus.NEW) {
            lead.setStatus(LeadPipelineStatus.ASSIGNED);
        }
        LeadActivity activity = new LeadActivity(LeadActivityType.ASSIGNMENT, assignedBy);
        activity.setSubject("Lead assigned");
        activity.setDetails("Assigned to " + assignee.getFullName());
        lead.addActivity(activity);
        leadRepository.saveAndFlush(lead);
        return leadMapper.toAssignmentResponse(assignment);
    }

    @Transactional
    public LeadResponse updateStatus(
            Long leadId,
            LeadStatusUpdateRequest request,
            AuthUserPrincipal actor
    ) {
        Lead lead = requireAccessibleLead(leadId, actor);
        LeadPipelineStatus current = lead.getStatus();
        LeadPipelineStatus target = request.status();
        if (target == LeadPipelineStatus.ASSIGNED) {
            throw new BusinessException("Use the assign endpoint to assign a lead");
        }
        if (current == target) {
            throw new BusinessException("Lead already has the requested status");
        }
        if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
            throw new BusinessException(
                    "Lead status cannot transition from " + current + " to " + target
            );
        }
        if ((target == LeadPipelineStatus.CLOSED_LOST
                || target == LeadPipelineStatus.INVALID)
                && request.reason() == null) {
            throw new BusinessException("reason is required for lost or invalid leads");
        }
        if (target != LeadPipelineStatus.INVALID && lead.getCurrentAssignee() == null) {
            throw new BusinessException("Lead must be assigned before pipeline progression");
        }

        Instant now = Instant.now();
        lead.setStatus(target);
        lead.setLostReason(
                target == LeadPipelineStatus.CLOSED_LOST
                        || target == LeadPipelineStatus.INVALID
                        ? request.reason()
                        : null
        );
        if (target == LeadPipelineStatus.CONTACTED) {
            lead.setLastContactedAt(now);
        }
        if (target == LeadPipelineStatus.CLOSED_WON) {
            lead.setConvertedAt(now);
            lead.setClosedAt(now);
        } else if (target == LeadPipelineStatus.CLOSED_LOST
                || target == LeadPipelineStatus.INVALID) {
            lead.setClosedAt(now);
        }

        User changedBy = requireUser(actor.id(), "Authenticated user not found");
        LeadActivity activity = new LeadActivity(LeadActivityType.STATUS_CHANGE, changedBy);
        activity.setSubject("Lead status changed");
        activity.setDetails(
                current + " -> " + target
                        + (request.reason() == null ? "" : ": " + request.reason())
        );
        lead.addActivity(activity);
        return leadMapper.toResponse(leadRepository.saveAndFlush(lead));
    }

    @Transactional
    public LeadNoteResponse addNote(
            Long leadId,
            LeadNoteRequest request,
            AuthUserPrincipal actor
    ) {
        Lead lead = requireAccessibleLead(leadId, actor);
        User author = requireUser(actor.id(), "Authenticated user not found");
        LeadNote note = new LeadNote(author, request.content());
        note.setPinned(request.pinned());
        lead.addNote(note);
        return leadMapper.toNoteResponse(noteRepository.saveAndFlush(note));
    }

    @Transactional
    public LeadActivityResponse addActivity(
            Long leadId,
            LeadActivityRequest request,
            AuthUserPrincipal actor
    ) {
        Lead lead = requireAccessibleLead(leadId, actor);
        requireOpenLead(lead);
        if (request.activityType() == LeadActivityType.STATUS_CHANGE
                || request.activityType() == LeadActivityType.ASSIGNMENT) {
            throw new BusinessException(
                    "System activity types cannot be created manually"
            );
        }
        User activityActor = requireUser(actor.id(), "Authenticated user not found");
        LeadActivity activity = new LeadActivity(request.activityType(), activityActor);
        activity.setSubject(request.subject());
        activity.setDetails(request.details());
        if (request.occurredAt() != null) {
            activity.setOccurredAt(request.occurredAt());
        }
        lead.addActivity(activity);
        if (request.activityType() == LeadActivityType.CALL
                || request.activityType() == LeadActivityType.EMAIL
                || request.activityType() == LeadActivityType.CHAT
                || request.activityType() == LeadActivityType.MEETING) {
            lead.setLastContactedAt(activity.getOccurredAt());
        }
        return leadMapper.toActivityResponse(activityRepository.saveAndFlush(activity));
    }

    @Transactional
    public FollowUpTaskResponse createFollowUpTask(
            Long leadId,
            FollowUpTaskRequest request,
            AuthUserPrincipal actor
    ) {
        Lead lead = requireAccessibleLead(leadId, actor);
        requireOpenLead(lead);
        User creator = requireUser(actor.id(), "Authenticated user not found");
        User assignee = resolveTaskAssignee(request.assignedAgentId(), lead, actor);
        FollowUpTask task = new FollowUpTask(
                request.title(),
                assignee,
                creator,
                request.dueAt()
        );
        task.setDescription(request.description());
        task.setPriority(request.priority());
        lead.addFollowUpTask(task);
        return leadMapper.toTaskResponse(taskRepository.saveAndFlush(task));
    }

    private Lead requireAccessibleLead(Long leadId, AuthUserPrincipal actor) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        if (lead.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Lead not found");
        }
        if (!isManagerOrAdmin(actor)) {
            boolean createdByActor = lead.getCreatedBy() != null
                    && lead.getCreatedBy().getId().equals(actor.id());
            boolean assignedToActor = lead.getCurrentAssignee() != null
                    && lead.getCurrentAssignee().getId().equals(actor.id());
            if (!createdByActor && !assignedToActor) {
                throw new AccessDeniedException(
                        "Agents can only access leads they created or are assigned"
                );
            }
        }
        return lead;
    }

    private Customer resolveCustomer(Long customerId, AuthUserPrincipal actor) {
        if (customerId == null) {
            return null;
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (customer.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Customer not found");
        }
        if (!isManagerOrAdmin(actor)) {
            boolean createdByActor = customer.getCreatedBy().getId().equals(actor.id());
            boolean assignedToActor = customer.getAssignedAgent() != null
                    && customer.getAssignedAgent().getId().equals(actor.id());
            if (!createdByActor && !assignedToActor) {
                throw new AccessDeniedException(
                        "Agents can only link accessible customers"
                );
            }
        }
        return customer;
    }

    private Listing resolveListing(Long listingId) {
        if (listingId == null) {
            return null;
        }
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (listing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Listing not found");
        }
        return listing;
    }

    private User resolveCreateAssignee(Long requestedAgentId, AuthUserPrincipal actor) {
        if (isManagerOrAdmin(actor)) {
            return requestedAgentId == null ? null : requireActiveAgent(requestedAgentId);
        }
        if (requestedAgentId != null && !requestedAgentId.equals(actor.id())) {
            throw new AccessDeniedException("Agents cannot assign leads to another agent");
        }
        return requireActiveAgent(actor.id());
    }

    private User resolveTaskAssignee(
            Long requestedAgentId,
            Lead lead,
            AuthUserPrincipal actor
    ) {
        if (!isManagerOrAdmin(actor)
                && requestedAgentId != null
                && !requestedAgentId.equals(actor.id())) {
            throw new AccessDeniedException("Agents cannot assign tasks to another agent");
        }
        Long assigneeId = requestedAgentId;
        if (assigneeId == null && lead.getCurrentAssignee() != null) {
            assigneeId = lead.getCurrentAssignee().getId();
        }
        if (assigneeId == null) {
            if (!actor.roles().contains(RoleCode.AGENT.name())) {
                throw new BusinessException("assignedAgentId is required for an unassigned lead");
            }
            assigneeId = actor.id();
        }
        return requireActiveAgent(assigneeId);
    }

    private User requireActiveAgent(Long userId) {
        User user = requireUser(userId, "Agent not found");
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

    private User requireUser(Long userId, String message) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(message));
    }

    private void requireOpenLead(Lead lead) {
        if (TERMINAL_STATUSES.contains(lead.getStatus())) {
            throw new BusinessException("Closed or invalid leads cannot be modified");
        }
    }

    private void requireManagerOrAdmin(AuthUserPrincipal actor, String message) {
        if (!isManagerOrAdmin(actor)) {
            throw new AccessDeniedException(message);
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }

    private static Map<LeadPipelineStatus, Set<LeadPipelineStatus>> buildTransitions() {
        Map<LeadPipelineStatus, Set<LeadPipelineStatus>> transitions =
                new EnumMap<>(LeadPipelineStatus.class);
        transitions.put(
                LeadPipelineStatus.NEW,
                Set.of(LeadPipelineStatus.INVALID)
        );
        transitions.put(
                LeadPipelineStatus.ASSIGNED,
                Set.of(LeadPipelineStatus.CONTACTED, LeadPipelineStatus.INVALID)
        );
        transitions.put(
                LeadPipelineStatus.CONTACTED,
                Set.of(
                        LeadPipelineStatus.INTERESTED,
                        LeadPipelineStatus.CLOSED_LOST,
                        LeadPipelineStatus.INVALID
                )
        );
        transitions.put(
                LeadPipelineStatus.INTERESTED,
                Set.of(
                        LeadPipelineStatus.VIEWING_SCHEDULED,
                        LeadPipelineStatus.NEGOTIATING,
                        LeadPipelineStatus.CLOSED_LOST
                )
        );
        transitions.put(
                LeadPipelineStatus.VIEWING_SCHEDULED,
                Set.of(
                        LeadPipelineStatus.INTERESTED,
                        LeadPipelineStatus.NEGOTIATING,
                        LeadPipelineStatus.CLOSED_LOST
                )
        );
        transitions.put(
                LeadPipelineStatus.NEGOTIATING,
                Set.of(
                        LeadPipelineStatus.CLOSED_WON,
                        LeadPipelineStatus.CLOSED_LOST
                )
        );
        transitions.put(LeadPipelineStatus.CLOSED_WON, Set.of());
        transitions.put(LeadPipelineStatus.CLOSED_LOST, Set.of());
        transitions.put(LeadPipelineStatus.INVALID, Set.of());
        return Map.copyOf(transitions);
    }
}
