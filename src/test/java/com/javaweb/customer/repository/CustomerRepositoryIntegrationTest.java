package com.javaweb.customer.repository;

import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerFavoriteListing;
import com.javaweb.customer.entity.CustomerNote;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.customer.entity.CustomerTag;
import com.javaweb.customer.enums.CustomerPriority;
import com.javaweb.customer.enums.CustomerSource;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryIntegrationTest {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerRequirementRepository requirementRepository;

    @Autowired
    private CustomerTagRepository tagRepository;

    @Autowired
    private CustomerNoteRepository noteRepository;

    @Autowired
    private CustomerFavoriteListingRepository favoriteRepository;

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
    private Listing listing;

    @BeforeEach
    void setUp() {
        agent = createUser("customer-schema-agent@example.test", RoleCode.AGENT);
        customerUser = createUser("customer-schema-user@example.test", RoleCode.CUSTOMER);

        Province province = provinceRepository.saveAndFlush(
                new Province("P-CUSTOMER-SCHEMA", "Customer Schema Province")
        );
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
        Property property = new Property(
                "PROP-CUSTOMER-SCHEMA",
                "Customer Schema Property",
                propertyType,
                new Address(province, "21 Customer Street"),
                agent,
                PropertyPurpose.SALE
        );
        property = propertyRepository.saveAndFlush(property);
        listing = new Listing(
                "LISTING-CUSTOMER-SCHEMA",
                property,
                agent,
                "Customer Schema Listing",
                "customer-schema-listing",
                "Listing used by customer schema integration test",
                ListingPurpose.SALE
        );
        listing.setStatus(ListingStatus.PUBLISHED);
        listing = listingRepository.saveAndFlush(listing);
    }

    @Test
    void shouldPersistCustomerWithOptionalAccountAndAllChildRecords() {
        Customer customer = new Customer("CUS-001", "Linked Customer", agent);
        customer.setUser(customerUser);
        customer.setAssignedAgent(agent);
        customer.setEmail(customerUser.getEmail());
        customer.setSource(CustomerSource.WEBSITE);
        customer.setPriority(CustomerPriority.HIGH);

        CustomerRequirement requirement = new CustomerRequirement(ListingPurpose.SALE);
        requirement.setPropertyType(propertyTypeRepository.findByCode("APARTMENT").orElseThrow());
        requirement.setMinBudget(new BigDecimal("2000000000"));
        requirement.setMaxBudget(new BigDecimal("5000000000"));
        customer.addRequirement(requirement);
        customer.addTag(new CustomerTag("VIP", agent));
        customer.addNote(new CustomerNote(agent, "Customer prefers riverside properties"));
        customer.addFavorite(new CustomerFavoriteListing(listing, agent));

        Customer saved = customerRepository.saveAndFlush(customer);

        assertThat(customerRepository.findByUserIdAndDeletedAtIsNull(customerUser.getId()))
                .contains(saved);
        assertThat(requirementRepository
                .findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(saved.getId()))
                .hasSize(1);
        assertThat(tagRepository.findAllByCustomerIdOrderByName(saved.getId()))
                .extracting(CustomerTag::getName)
                .containsExactly("VIP");
        assertThat(noteRepository
                .findAllByCustomerIdOrderByPinnedDescCreatedAtDesc(
                        saved.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10)
                ))
                .hasSize(1);
        assertThat(favoriteRepository.existsByCustomerIdAndListingId(
                saved.getId(),
                listing.getId()
        )).isTrue();
    }

    @Test
    void shouldAllowCustomerWithoutAccountAndEnforceUniqueLinks() {
        Customer contact = new Customer("CUS-CONTACT", "Offline Contact", agent);
        contact.setPhone("0900000021");
        customerRepository.saveAndFlush(contact);

        assertThat(contact.getUser()).isNull();

        Customer first = new Customer("CUS-LINK-1", "First Linked Customer", agent);
        first.setUser(customerUser);
        customerRepository.saveAndFlush(first);

        Customer duplicate = new Customer("CUS-LINK-2", "Duplicate Linked Customer", agent);
        duplicate.setUser(customerUser);
        assertThatThrownBy(() -> customerRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCascadeDeleteCustomerOwnedRecords() {
        Customer customer = new Customer("CUS-CASCADE", "Cascade Customer", agent);
        customer.setEmail("cascade-customer@example.test");
        customer.addRequirement(new CustomerRequirement(ListingPurpose.RENT));
        customer.addTag(new CustomerTag("FOLLOW_UP", agent));
        customer.addNote(new CustomerNote(agent, "Call next week"));
        customer.addFavorite(new CustomerFavoriteListing(listing, agent));
        customer = customerRepository.saveAndFlush(customer);
        Long customerId = customer.getId();

        customerRepository.delete(customer);
        customerRepository.flush();

        assertThat(requirementRepository
                .findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(customerId))
                .isEmpty();
        assertThat(tagRepository.findAllByCustomerIdOrderByName(customerId)).isEmpty();
        assertThat(noteRepository
                .findAllByCustomerIdOrderByPinnedDescCreatedAtDesc(
                        customerId,
                        org.springframework.data.domain.PageRequest.of(0, 10)
                ))
                .isEmpty();
        assertThat(favoriteRepository
                .findAllByCustomerIdOrderByCreatedAtDesc(
                        customerId,
                        org.springframework.data.domain.PageRequest.of(0, 10)
                ))
                .isEmpty();
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, "encoded-password", roleCode + " Customer Schema User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }
}
