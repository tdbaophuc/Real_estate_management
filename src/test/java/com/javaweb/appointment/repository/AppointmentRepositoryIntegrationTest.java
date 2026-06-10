package com.javaweb.appointment.repository;

import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.entity.AppointmentParticipant;
import com.javaweb.appointment.entity.ViewingFeedback;
import com.javaweb.appointment.enums.AppointmentParticipantRole;
import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.appointment.enums.ViewingInterestLevel;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class AppointmentRepositoryIntegrationTest {
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentParticipantRepository participantRepository;

    @Autowired
    private ViewingFeedbackRepository feedbackRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User agent;
    private User customerUser;
    private User owner;
    private Customer customer;
    private Property property;
    private Listing listing;
    private Instant startAt;
    private Instant endAt;

    @BeforeEach
    void setUp() {
        agent = createUser("appointment-schema-agent@example.test", RoleCode.AGENT);
        customerUser = createUser(
                "appointment-schema-customer@example.test",
                RoleCode.CUSTOMER
        );
        owner = createUser("appointment-schema-owner@example.test", RoleCode.OWNER);

        Province province = provinceRepository.saveAndFlush(
                new Province("P-APPOINTMENT-SCHEMA", "Appointment Schema Province")
        );
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT")
                .orElseThrow();
        property = new Property(
                "PROP-APPOINTMENT-SCHEMA",
                "Appointment Schema Property",
                propertyType,
                new Address(province, "25 Appointment Street"),
                agent,
                PropertyPurpose.SALE
        );
        property.setOwner(owner);
        property = propertyRepository.saveAndFlush(property);
        listing = new Listing(
                "LISTING-APPOINTMENT-SCHEMA",
                property,
                agent,
                "Appointment Schema Listing",
                "appointment-schema-listing",
                "Listing used by appointment schema integration test",
                ListingPurpose.SALE
        );
        listing.setStatus(ListingStatus.PUBLISHED);
        listing = listingRepository.saveAndFlush(listing);

        customer = new Customer("CUS-APPOINTMENT-SCHEMA", "Appointment Customer", agent);
        customer.setUser(customerUser);
        customer.setAssignedAgent(agent);
        customer.setEmail(customerUser.getEmail());
        customer = customerRepository.saveAndFlush(customer);

        startAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        endAt = startAt.plus(1, ChronoUnit.HOURS);
    }

    @Test
    void shouldPersistAppointmentWithAllRequiredLinksParticipantsAndFeedback() {
        Appointment appointment = new Appointment(
                "APT-001",
                customer,
                agent,
                property,
                agent,
                "Property viewing",
                startAt,
                endAt
        );
        appointment.setListing(listing);
        appointment.setMeetingLocation("Property lobby");
        appointment.addParticipant(new AppointmentParticipant(
                customerUser,
                AppointmentParticipantRole.CUSTOMER
        ));
        appointment.addParticipant(new AppointmentParticipant(
                owner,
                AppointmentParticipantRole.OWNER
        ));
        ViewingFeedback feedback = new ViewingFeedback(
                agent,
                ViewingInterestLevel.HIGH
        );
        feedback.setRating(5);
        feedback.setComments("Customer requested price negotiation");
        appointment.addFeedback(feedback);

        Appointment saved = appointmentRepository.saveAndFlush(appointment);

        assertThat(appointmentRepository.findByCode("APT-001")).contains(saved);
        assertThat(saved.getCustomer()).isEqualTo(customer);
        assertThat(saved.getAgent()).isEqualTo(agent);
        assertThat(saved.getProperty()).isEqualTo(property);
        assertThat(saved.getListing()).isEqualTo(listing);
        assertThat(participantRepository
                .findAllByAppointmentIdOrderByCreatedAtAsc(saved.getId()))
                .hasSize(2);
        assertThat(feedbackRepository
                .findAllByAppointmentIdOrderByCreatedAtDesc(saved.getId()))
                .extracting(ViewingFeedback::getInterestLevel)
                .containsExactly(ViewingInterestLevel.HIGH);
    }

    @Test
    void shouldDetectOverlappingAgentAndPropertyAppointments() {
        Appointment appointment = new Appointment(
                "APT-CONFLICT",
                customer,
                agent,
                property,
                agent,
                "Conflict source",
                startAt,
                endAt
        );
        appointmentRepository.saveAndFlush(appointment);
        List<AppointmentStatus> excluded = List.of(
                AppointmentStatus.CANCELLED,
                AppointmentStatus.RESCHEDULED
        );

        assertThat(appointmentRepository.existsAgentConflict(
                agent.getId(),
                startAt.plus(30, ChronoUnit.MINUTES),
                endAt.plus(30, ChronoUnit.MINUTES),
                excluded
        )).isTrue();
        assertThat(appointmentRepository.existsPropertyConflict(
                property.getId(),
                startAt.minus(30, ChronoUnit.MINUTES),
                startAt.plus(30, ChronoUnit.MINUTES),
                excluded
        )).isTrue();
        assertThat(appointmentRepository.existsAgentConflict(
                agent.getId(),
                endAt,
                endAt.plus(1, ChronoUnit.HOURS),
                excluded
        )).isFalse();
    }

    @Test
    void shouldEnforceAppointmentTimeRange() {
        Appointment invalid = new Appointment(
                "APT-TIME",
                customer,
                agent,
                property,
                agent,
                "Invalid appointment",
                endAt,
                startAt
        );

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceUniqueParticipantPerAppointment() {
        Appointment appointment = new Appointment(
                "APT-PARTICIPANT",
                customer,
                agent,
                property,
                agent,
                "Participant uniqueness",
                startAt,
                endAt
        );
        appointment.addParticipant(new AppointmentParticipant(
                customerUser,
                AppointmentParticipantRole.CUSTOMER
        ));
        appointment.addParticipant(new AppointmentParticipant(
                customerUser,
                AppointmentParticipantRole.OTHER
        ));

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(appointment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCascadeDeleteParticipantsAndFeedbacks() {
        Appointment appointment = new Appointment(
                "APT-CASCADE",
                customer,
                agent,
                property,
                agent,
                "Cascade appointment",
                startAt,
                endAt
        );
        appointment.addParticipant(new AppointmentParticipant(
                owner,
                AppointmentParticipantRole.OWNER
        ));
        appointment.addFeedback(new ViewingFeedback(
                agent,
                ViewingInterestLevel.MEDIUM
        ));
        appointment = appointmentRepository.saveAndFlush(appointment);
        Long appointmentId = appointment.getId();

        appointmentRepository.delete(appointment);
        appointmentRepository.flush();

        assertThat(participantRepository
                .findAllByAppointmentIdOrderByCreatedAtAsc(appointmentId))
                .isEmpty();
        assertThat(feedbackRepository
                .findAllByAppointmentIdOrderByCreatedAtDesc(appointmentId))
                .isEmpty();
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, "encoded-password", roleCode + " Appointment User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }
}
