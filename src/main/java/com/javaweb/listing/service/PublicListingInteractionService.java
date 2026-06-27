package com.javaweb.listing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.appointment.dto.AppointmentResponse;
import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.entity.AppointmentParticipant;
import com.javaweb.appointment.enums.AppointmentParticipantRole;
import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.appointment.mapper.AppointmentMapper;
import com.javaweb.appointment.repository.AppointmentRepository;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.enums.CustomerSource;
import com.javaweb.customer.enums.CustomerStatus;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.lead.dto.LeadResponse;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadActivity;
import com.javaweb.lead.entity.LeadAssignment;
import com.javaweb.lead.entity.LeadSource;
import com.javaweb.lead.enums.LeadActivityType;
import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.lead.enums.LeadPriority;
import com.javaweb.lead.mapper.LeadMapper;
import com.javaweb.lead.repository.LeadRepository;
import com.javaweb.lead.repository.LeadSourceRepository;
import com.javaweb.listing.dto.ListingAppointmentRequest;
import com.javaweb.listing.dto.ListingInquiryRequest;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.notification.entity.Notification;
import com.javaweb.notification.repository.NotificationRepository;
import com.javaweb.property.entity.Property;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PublicListingInteractionService {
    private static final String LISTING_INQUIRY_SOURCE = "LISTING_INQUIRY";

    private final ListingRepository listingRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final LeadSourceRepository leadSourceRepository;
    private final LeadRepository leadRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final LeadMapper leadMapper;
    private final AppointmentMapper appointmentMapper;
    private final ObjectMapper objectMapper;

    public PublicListingInteractionService(
            ListingRepository listingRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            LeadSourceRepository leadSourceRepository,
            LeadRepository leadRepository,
            AppointmentRepository appointmentRepository,
            NotificationRepository notificationRepository,
            LeadMapper leadMapper,
            AppointmentMapper appointmentMapper,
            ObjectMapper objectMapper
    ) {
        this.listingRepository = listingRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.leadSourceRepository = leadSourceRepository;
        this.leadRepository = leadRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationRepository = notificationRepository;
        this.leadMapper = leadMapper;
        this.appointmentMapper = appointmentMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LeadResponse createInquiry(
            Long listingId,
            ListingInquiryRequest request,
            AuthUserPrincipal principal
    ) {
        Listing listing = requirePublicListing(listingId);
        User agent = resolveListingAgent(listing);
        Customer customer = resolveCustomer(
                request.fullName(),
                request.email(),
                request.phone(),
                request.preferredContactMethod(),
                request.message(),
                agent,
                principal
        );
        Lead lead = createLead(
                listing,
                customer,
                agent,
                request.fullName(),
                request.email(),
                request.phone(),
                request.message()
        );
        notifyAgent(
                agent,
                "LISTING_INQUIRY",
                "New listing inquiry",
                request.fullName() + " asked about " + listing.getTitle(),
                "/leads/" + lead.getId(),
                "LEAD",
                lead.getId(),
                Map.of("listingId", listing.getId(), "customerId", customer.getId())
        );
        return leadMapper.toResponse(lead);
    }

    @Transactional
    public AppointmentResponse createAppointmentRequest(
            Long listingId,
            ListingAppointmentRequest request,
            AuthUserPrincipal principal
    ) {
        Listing listing = requirePublicListing(listingId);
        User agent = resolveListingAgent(listing);
        Customer customer = resolveCustomer(
                request.fullName(),
                request.email(),
                request.phone(),
                null,
                request.message(),
                agent,
                principal
        );
        Lead lead = createLead(
                listing,
                customer,
                agent,
                request.fullName(),
                request.email(),
                request.phone(),
                request.message() == null
                        ? "Appointment request from public listing"
                        : request.message()
        );
        Appointment appointment = new Appointment(
                uniqueCode("APT-PUB"),
                customer,
                agent,
                listing.getProperty(),
                agent,
                "Viewing request: " + listing.getTitle(),
                request.preferredStartAt(),
                request.preferredEndAt()
        );
        appointment.setListing(listing);
        appointment.setLead(lead);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setNotes(request.message());
        appointment.addParticipant(new AppointmentParticipant(
                agent,
                AppointmentParticipantRole.AGENT
        ));
        if (customer.getUser() != null && !customer.getUser().getId().equals(agent.getId())) {
            appointment.addParticipant(new AppointmentParticipant(
                    customer.getUser(),
                    AppointmentParticipantRole.CUSTOMER
            ));
        }
        Appointment saved = appointmentRepository.saveAndFlush(appointment);
        notifyAgent(
                agent,
                "APPOINTMENT_REQUEST",
                "New appointment request",
                request.fullName() + " requested a viewing for " + listing.getTitle(),
                "/appointments/" + saved.getId(),
                "APPOINTMENT",
                saved.getId(),
                Map.of(
                        "listingId", listing.getId(),
                        "leadId", lead.getId(),
                        "customerId", customer.getId()
                )
        );
        return appointmentMapper.toResponse(saved);
    }

    private Listing requirePublicListing(Long listingId) {
        Listing listing = listingRepository.findPublicInteractionById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Published listing not found"));
        if (listing.getStatus() != ListingStatus.PUBLISHED
                || listing.getVisibility() != ListingVisibility.PUBLIC
                || listing.getDeletedAt() != null
                || listing.getProperty().getDeletedAt() != null) {
            throw new ResourceNotFoundException("Published listing not found");
        }
        return listing;
    }

    private User resolveListingAgent(Listing listing) {
        Property property = listing.getProperty();
        User assignedAgent = property.getAssignedAgent();
        if (isActiveAgent(assignedAgent)) {
            return assignedAgent;
        }
        if (isActiveAgent(listing.getCreatedBy())) {
            return listing.getCreatedBy();
        }
        throw new BusinessException("Listing does not have an active assigned agent");
    }

    private Customer resolveCustomer(
            String fullName,
            String email,
            String phone,
            String preferredContactMethod,
            String message,
            User agent,
            AuthUserPrincipal principal
    ) {
        Customer customer = findExistingCustomer(email, phone);
        User linkedUser = resolveLinkedCustomerUser(principal);
        if (customer == null && linkedUser != null) {
            customer = customerRepository.findByUserIdAndDeletedAtIsNull(linkedUser.getId())
                    .orElse(null);
        }
        if (customer == null) {
            customer = new Customer(uniqueCode("CUS-PUB"), fullName, agent);
            customer.setSource(CustomerSource.WEBSITE);
            customer.setStatus(CustomerStatus.ACTIVE);
        }
        if (customer.getEmail() == null && email != null) {
            customer.setEmail(email);
        }
        if (customer.getPhone() == null && phone != null) {
            customer.setPhone(phone);
        }
        if (customer.getPreferredContactMethod() == null && preferredContactMethod != null) {
            customer.setPreferredContactMethod(preferredContactMethod);
        }
        if (customer.getAssignedAgent() == null) {
            customer.setAssignedAgent(agent);
        }
        if (customer.getUser() == null
                && linkedUser != null
                && canLinkCustomerUser(customer, linkedUser)) {
            customer.setUser(linkedUser);
        }
        customer.setNotes(appendNote(customer.getNotes(), message));
        return customerRepository.saveAndFlush(customer);
    }

    private boolean canLinkCustomerUser(Customer customer, User linkedUser) {
        return customerRepository.findByUserIdAndDeletedAtIsNull(linkedUser.getId())
                .map(existing -> existing.getId().equals(customer.getId()))
                .orElse(true);
    }

    private Customer findExistingCustomer(String email, String phone) {
        if (email != null) {
            Customer customer = customerRepository
                    .findFirstByEmailIgnoreCaseAndDeletedAtIsNull(email)
                    .orElse(null);
            if (customer != null) {
                return customer;
            }
        }
        if (phone != null) {
            return customerRepository.findFirstByPhoneAndDeletedAtIsNull(phone)
                    .orElse(null);
        }
        return null;
    }

    private User resolveLinkedCustomerUser(AuthUserPrincipal principal) {
        if (principal == null || !principal.roles().contains(RoleCode.CUSTOMER.name())) {
            return null;
        }
        return userRepository.findWithRolesById(principal.id())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(null);
    }

    private Lead createLead(
            Listing listing,
            Customer customer,
            User agent,
            String fullName,
            String email,
            String phone,
            String message
    ) {
        LeadSource source = leadSourceRepository.findByCodeAndActiveTrue(LISTING_INQUIRY_SOURCE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Listing inquiry lead source not found"
                ));
        Lead lead = new Lead(uniqueCode("LEAD-LST"), source, fullName);
        lead.setCustomer(customer);
        lead.setListing(listing);
        lead.setCreatedBy(agent);
        lead.setCurrentAssignee(agent);
        lead.setStatus(LeadPipelineStatus.ASSIGNED);
        lead.setPriority(LeadPriority.MEDIUM);
        lead.setEmail(email);
        lead.setPhone(phone);
        lead.setMessage(message);
        lead.addAssignment(new LeadAssignment(agent, agent));
        LeadActivity activity = new LeadActivity(LeadActivityType.ASSIGNMENT, agent);
        activity.setSubject("Lead assigned from listing request");
        activity.setDetails("Assigned to " + agent.getFullName());
        lead.addActivity(activity);
        return leadRepository.saveAndFlush(lead);
    }

    private void notifyAgent(
            User agent,
            String type,
            String title,
            String message,
            String actionUrl,
            String referenceType,
            Long referenceId,
            Map<String, Object> metadata
    ) {
        Notification notification = new Notification(agent, type, title, message);
        notification.setActionUrl(actionUrl);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setMetadataJson(toJson(metadata));
        notificationRepository.save(notification);
    }

    private boolean isActiveAgent(User user) {
        return user != null
                && user.getStatus() == UserStatus.ACTIVE
                && user.getRoles().stream()
                        .map(Role::getCode)
                        .anyMatch(RoleCode.AGENT::equals);
    }

    private String appendNote(String existing, String message) {
        if (message == null || message.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return message;
        }
        String combined = existing + "\n\n" + message;
        return combined.length() <= 2000 ? combined : combined.substring(0, 2000);
    }

    private String uniqueCode(String prefix) {
        return prefix + "-" + Instant.now().toEpochMilli() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String toJson(Map<String, Object> metadata) {
        Map<String, Object> value = new LinkedHashMap<>(metadata);
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize notification metadata", exception);
        }
    }
}
