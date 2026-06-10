package com.javaweb.appointment.service;

import com.javaweb.appointment.dto.AppointmentCancelRequest;
import com.javaweb.appointment.dto.AppointmentCreateRequest;
import com.javaweb.appointment.dto.AppointmentRescheduleRequest;
import com.javaweb.appointment.dto.AppointmentResponse;
import com.javaweb.appointment.dto.AppointmentSearchRequest;
import com.javaweb.appointment.dto.ViewingFeedbackRequest;
import com.javaweb.appointment.dto.ViewingFeedbackResponse;
import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.entity.AppointmentParticipant;
import com.javaweb.appointment.entity.ViewingFeedback;
import com.javaweb.appointment.enums.AppointmentParticipantRole;
import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.appointment.enums.ParticipantResponseStatus;
import com.javaweb.appointment.mapper.AppointmentMapper;
import com.javaweb.appointment.repository.AppointmentParticipantRepository;
import com.javaweb.appointment.repository.AppointmentRepository;
import com.javaweb.appointment.repository.AppointmentSpecifications;
import com.javaweb.appointment.repository.ViewingFeedbackRepository;
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
import com.javaweb.customer.enums.CustomerStatus;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.repository.LeadRepository;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.property.entity.Property;
import com.javaweb.property.enums.PropertyStatus;
import com.javaweb.property.repository.PropertyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AppointmentService {
    private static final Set<AppointmentStatus> CONFLICT_EXCLUDED_STATUSES = Set.of(
            AppointmentStatus.CANCELLED,
            AppointmentStatus.RESCHEDULED
    );
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "code", "code",
            "status", "status",
            "startAt", "startAt",
            "endAt", "endAt",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private final AppointmentRepository appointmentRepository;
    private final AppointmentParticipantRepository participantRepository;
    private final ViewingFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final ListingRepository listingRepository;
    private final LeadRepository leadRepository;
    private final AppointmentMapper appointmentMapper;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentParticipantRepository participantRepository,
            ViewingFeedbackRepository feedbackRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            PropertyRepository propertyRepository,
            ListingRepository listingRepository,
            LeadRepository leadRepository,
            AppointmentMapper appointmentMapper
    ) {
        this.appointmentRepository = appointmentRepository;
        this.participantRepository = participantRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.listingRepository = listingRepository;
        this.leadRepository = leadRepository;
        this.appointmentMapper = appointmentMapper;
    }

    @Transactional
    public AppointmentResponse create(
            AppointmentCreateRequest request,
            AuthUserPrincipal actor
    ) {
        if (appointmentRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Appointment code already exists");
        }
        User creator = requireUser(actor.id(), "Authenticated user not found");
        Customer customer = requireCustomer(request.customerId());
        User agent = requireActiveAgent(request.agentId());
        Property property = requireAvailableProperty(request.propertyId());
        Listing listing = resolveListing(request.listingId(), property);
        Lead lead = resolveLead(request.leadId(), customer, agent, listing);
        requireCanCreate(customer, agent, actor);
        requireNoConflict(agent.getId(), property.getId(), request.startAt(), request.endAt());

        Appointment appointment = new Appointment(
                request.code(),
                customer,
                agent,
                property,
                creator,
                request.title(),
                request.startAt(),
                request.endAt()
        );
        appointment.setListing(listing);
        appointment.setLead(lead);
        appointment.setTimezone(
                request.timezone() == null ? "Asia/Ho_Chi_Minh" : request.timezone()
        );
        appointment.setMeetingLocation(request.meetingLocation());
        appointment.setNotes(request.notes());
        addDefaultParticipants(appointment);
        return appointmentMapper.toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> search(
            AppointmentSearchRequest request,
            AuthUserPrincipal actor
    ) {
        requireStaff(actor);
        String sortField = SORT_FIELDS.get(request.sortBy());
        if (sortField == null) {
            throw new BusinessException("Unsupported appointment sort field");
        }
        Long visibleAgentId = isManagerOrAdmin(actor) ? null : actor.id();
        Page<Appointment> page = appointmentRepository.findAll(
                AppointmentSpecifications.search(request, visibleAgentId),
                PageRequest.of(
                        request.page(),
                        request.size(),
                        Sort.by(request.direction(), sortField)
                                .and(Sort.by(Sort.Direction.ASC, "id"))
                )
        );
        return PageResponse.from(
                page,
                page.getContent().stream().map(appointmentMapper::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> listMy(
            int pageNumber,
            int pageSize,
            AuthUserPrincipal actor
    ) {
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        Page<Appointment> page;
        if (actor.roles().contains(RoleCode.CUSTOMER.name())) {
            Customer customer = customerRepository.findByUserIdAndDeletedAtIsNull(actor.id())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer profile not found"
                    ));
            page = appointmentRepository.findAllByCustomerIdOrderByStartAtDesc(
                    customer.getId(),
                    pageable
            );
        } else if (actor.roles().contains(RoleCode.AGENT.name())) {
            page = appointmentRepository.findAllByAgentIdOrderByStartAtDesc(
                    actor.id(),
                    pageable
            );
        } else {
            page = appointmentRepository.findAllByCreatedByIdOrderByStartAtDesc(
                    actor.id(),
                    pageable
            );
        }
        return PageResponse.from(
                page,
                page.getContent().stream().map(appointmentMapper::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long appointmentId, AuthUserPrincipal actor) {
        return appointmentMapper.toResponse(requireAccessible(appointmentId, actor));
    }

    @Transactional
    public AppointmentResponse confirm(Long appointmentId, AuthUserPrincipal actor) {
        Appointment appointment = requireAccessible(appointmentId, actor);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BusinessException("Only pending appointments can be confirmed");
        }
        Instant now = Instant.now();
        appointment.setConfirmedAt(now);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        updateParticipantResponse(
                appointmentId,
                actor.id(),
                ParticipantResponseStatus.ACCEPTED,
                now
        );
        return appointmentMapper.toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional
    public AppointmentResponse cancel(
            Long appointmentId,
            AppointmentCancelRequest request,
            AuthUserPrincipal actor
    ) {
        Appointment appointment = requireAccessible(appointmentId, actor);
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException(
                    "Only pending or confirmed appointments can be cancelled"
            );
        }
        Instant now = Instant.now();
        appointment.setCancellationReason(request.reason());
        appointment.setCancelledBy(requireUser(actor.id(), "Authenticated user not found"));
        appointment.setCancelledAt(now);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        updateParticipantResponse(
                appointmentId,
                actor.id(),
                ParticipantResponseStatus.DECLINED,
                now
        );
        return appointmentMapper.toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional
    public AppointmentResponse reschedule(
            Long appointmentId,
            AppointmentRescheduleRequest request,
            AuthUserPrincipal actor
    ) {
        Appointment current = requireAccessible(appointmentId, actor);
        if (current.getStatus() != AppointmentStatus.PENDING
                && current.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException(
                    "Only pending or confirmed appointments can be rescheduled"
            );
        }
        requireNoConflictExcluding(
                current.getId(),
                current.getAgent().getId(),
                current.getProperty().getId(),
                request.startAt(),
                request.endAt()
        );
        Appointment replacement = new Appointment(
                nextRescheduleCode(current.getCode()),
                current.getCustomer(),
                current.getAgent(),
                current.getProperty(),
                requireUser(actor.id(), "Authenticated user not found"),
                current.getTitle(),
                request.startAt(),
                request.endAt()
        );
        replacement.setLead(current.getLead());
        replacement.setListing(current.getListing());
        replacement.setRescheduledFrom(current);
        replacement.setTimezone(
                request.timezone() == null ? current.getTimezone() : request.timezone()
        );
        replacement.setMeetingLocation(
                request.meetingLocation() == null
                        ? current.getMeetingLocation()
                        : request.meetingLocation()
        );
        replacement.setNotes(request.notes() == null ? current.getNotes() : request.notes());
        for (AppointmentParticipant participant : current.getParticipants()) {
            replacement.addParticipant(new AppointmentParticipant(
                    participant.getUser(),
                    participant.getParticipantRole()
            ));
        }
        current.setStatus(AppointmentStatus.RESCHEDULED);
        appointmentRepository.saveAndFlush(current);
        return appointmentMapper.toResponse(
                appointmentRepository.saveAndFlush(replacement)
        );
    }

    @Transactional
    public AppointmentResponse complete(Long appointmentId, AuthUserPrincipal actor) {
        Appointment appointment = requireAccessible(appointmentId, actor);
        requireAgentOrManager(actor, appointment);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException("Only confirmed appointments can be completed");
        }
        appointment.setCompletedAt(Instant.now());
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentMapper.toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional
    public ViewingFeedbackResponse addFeedback(
            Long appointmentId,
            ViewingFeedbackRequest request,
            AuthUserPrincipal actor
    ) {
        Appointment appointment = requireAccessible(appointmentId, actor);
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessException(
                    "Feedback can only be added to completed appointments"
            );
        }
        if (feedbackRepository.findByAppointmentIdAndSubmittedById(
                appointmentId,
                actor.id()
        ).isPresent()) {
            throw new DuplicateResourceException(
                    "Feedback has already been submitted for this appointment"
            );
        }
        User submitter = requireUser(actor.id(), "Authenticated user not found");
        ViewingFeedback feedback = new ViewingFeedback(
                submitter,
                request.interestLevel()
        );
        feedback.setRating(request.rating());
        feedback.setComments(request.comments());
        feedback.setPositivePoints(request.positivePoints());
        feedback.setConcerns(request.concerns());
        feedback.setNextAction(request.nextAction());
        appointment.addFeedback(feedback);
        return appointmentMapper.toFeedbackResponse(
                feedbackRepository.saveAndFlush(feedback)
        );
    }

    private Appointment requireAccessible(Long appointmentId, AuthUserPrincipal actor) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (isManagerOrAdmin(actor)) {
            return appointment;
        }
        boolean assignedAgent = appointment.getAgent().getId().equals(actor.id());
        boolean linkedCustomer = appointment.getCustomer().getUser() != null
                && appointment.getCustomer().getUser().getId().equals(actor.id());
        if (!assignedAgent && !linkedCustomer) {
            throw new AccessDeniedException(
                    "You can only access appointments assigned or linked to you"
            );
        }
        return appointment;
    }

    private Customer requireCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (customer.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Customer not found");
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessException("Customer must be active");
        }
        return customer;
    }

    private Property requireAvailableProperty(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (property.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Property not found");
        }
        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            throw new BusinessException("Property must be available for viewing");
        }
        return property;
    }

    private Listing resolveListing(Long listingId, Property property) {
        if (listingId == null) {
            return null;
        }
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (listing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Listing not found");
        }
        if (!listing.getProperty().getId().equals(property.getId())) {
            throw new BusinessException("Listing must belong to the selected property");
        }
        return listing;
    }

    private Lead resolveLead(
            Long leadId,
            Customer customer,
            User agent,
            Listing listing
    ) {
        if (leadId == null) {
            return null;
        }
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        if (lead.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Lead not found");
        }
        if (lead.getCustomer() != null
                && !lead.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException("Lead must belong to the selected customer");
        }
        if (lead.getCurrentAssignee() != null
                && !lead.getCurrentAssignee().getId().equals(agent.getId())) {
            throw new BusinessException("Lead must be assigned to the selected agent");
        }
        if (listing != null && lead.getListing() != null
                && !lead.getListing().getId().equals(listing.getId())) {
            throw new BusinessException("Lead must reference the selected listing");
        }
        return lead;
    }

    private void requireCanCreate(
            Customer customer,
            User agent,
            AuthUserPrincipal actor
    ) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        if (actor.roles().contains(RoleCode.AGENT.name())) {
            if (!agent.getId().equals(actor.id())) {
                throw new AccessDeniedException(
                        "Agents cannot create appointments for another agent"
                );
            }
            boolean accessibleCustomer = customer.getCreatedBy().getId().equals(actor.id())
                    || customer.getAssignedAgent() != null
                    && customer.getAssignedAgent().getId().equals(actor.id());
            if (!accessibleCustomer) {
                throw new AccessDeniedException(
                        "Agents can only book appointments for accessible customers"
                );
            }
            return;
        }
        if (actor.roles().contains(RoleCode.CUSTOMER.name())
                && customer.getUser() != null
                && customer.getUser().getId().equals(actor.id())) {
            return;
        }
        throw new AccessDeniedException("You cannot create this appointment");
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

    private void addDefaultParticipants(Appointment appointment) {
        appointment.addParticipant(new AppointmentParticipant(
                appointment.getAgent(),
                AppointmentParticipantRole.AGENT
        ));
        User customerUser = appointment.getCustomer().getUser();
        if (customerUser != null
                && !customerUser.getId().equals(appointment.getAgent().getId())) {
            appointment.addParticipant(new AppointmentParticipant(
                    customerUser,
                    AppointmentParticipantRole.CUSTOMER
            ));
        }
    }

    private void updateParticipantResponse(
            Long appointmentId,
            Long userId,
            ParticipantResponseStatus status,
            Instant respondedAt
    ) {
        participantRepository.findByAppointmentIdAndUserId(appointmentId, userId)
                .ifPresent(participant -> {
                    participant.setResponseStatus(status);
                    participant.setRespondedAt(respondedAt);
                });
    }

    private void requireNoConflict(
            Long agentId,
            Long propertyId,
            Instant startAt,
            Instant endAt
    ) {
        if (appointmentRepository.existsAgentConflict(
                agentId,
                startAt,
                endAt,
                CONFLICT_EXCLUDED_STATUSES
        )) {
            throw new DuplicateResourceException(
                    "Agent already has an overlapping appointment"
            );
        }
        if (appointmentRepository.existsPropertyConflict(
                propertyId,
                startAt,
                endAt,
                CONFLICT_EXCLUDED_STATUSES
        )) {
            throw new DuplicateResourceException(
                    "Property already has an overlapping appointment"
            );
        }
    }

    private void requireNoConflictExcluding(
            Long currentId,
            Long agentId,
            Long propertyId,
            Instant startAt,
            Instant endAt
    ) {
        List<Appointment> overlaps = appointmentRepository.findAll(
                (root, query, builder) -> builder.and(
                        builder.notEqual(root.get("id"), currentId),
                        builder.not(root.get("status").in(CONFLICT_EXCLUDED_STATUSES)),
                        builder.or(
                                builder.equal(root.get("agent").get("id"), agentId),
                                builder.equal(root.get("property").get("id"), propertyId)
                        ),
                        builder.lessThan(root.get("startAt"), endAt),
                        builder.greaterThan(root.get("endAt"), startAt)
                )
        );
        if (!overlaps.isEmpty()) {
            boolean agentConflict = overlaps.stream()
                    .anyMatch(item -> item.getAgent().getId().equals(agentId));
            throw new DuplicateResourceException(
                    agentConflict
                            ? "Agent already has an overlapping appointment"
                            : "Property already has an overlapping appointment"
            );
        }
    }

    private String nextRescheduleCode(String originalCode) {
        String suffix = "-R-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
        String prefix = originalCode.substring(
                0,
                Math.min(originalCode.length(), 50 - suffix.length())
        );
        return prefix + suffix;
    }

    private void requireStaff(AuthUserPrincipal actor) {
        if (!actor.roles().contains(RoleCode.AGENT.name()) && !isManagerOrAdmin(actor)) {
            throw new AccessDeniedException("Only staff can list all appointments");
        }
    }

    private void requireAgentOrManager(
            AuthUserPrincipal actor,
            Appointment appointment
    ) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        if (!actor.roles().contains(RoleCode.AGENT.name())
                || !appointment.getAgent().getId().equals(actor.id())) {
            throw new AccessDeniedException(
                    "Only the assigned agent or management can complete appointments"
            );
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }
}
