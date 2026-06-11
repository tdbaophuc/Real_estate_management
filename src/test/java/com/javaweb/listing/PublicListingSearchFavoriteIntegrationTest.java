package com.javaweb.listing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.entity.ListingView;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import com.javaweb.listing.repository.ListingFavoriteRepository;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.listing.repository.ListingViewRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:public_listing_day20_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicListingSearchFavoriteIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingFavoriteRepository favoriteRepository;

    @Autowired
    private ListingViewRepository listingViewRepository;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User agent;
    private User customer;
    private PropertyType apartment;
    private Province city;
    private District centralDistrict;
    private Ward riversideWard;
    private Listing riverside;
    private Listing downtown;
    private Listing draft;
    private Listing internal;
    private String customerToken;
    private String agentToken;

    @BeforeEach
    void setUp() throws Exception {
        listingRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("day20-agent@example.test", RoleCode.AGENT);
        customer = createUser("day20-customer@example.test", RoleCode.CUSTOMER);
        agentToken = login(agent.getEmail());
        customerToken = login(customer.getEmail());

        apartment = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
        PropertyType house = propertyTypeRepository.findByCode("HOUSE").orElseThrow();

        city = new Province("P-D20-CITY", "Day 20 City");
        centralDistrict = new District(city, "D-D20-CENTRAL", "Central District");
        District westDistrict = new District(city, "D-D20-WEST", "West District");
        riversideWard = new Ward(
                centralDistrict,
                "W-D20-RIVER",
                "Riverside Ward"
        );
        Ward downtownWard = new Ward(westDistrict, "W-D20-DOWN", "Downtown Ward");
        city.addDistrict(centralDistrict);
        city.addDistrict(westDistrict);
        centralDistrict.addWard(riversideWard);
        westDistrict.addWard(downtownWard);
        provinceRepository.saveAndFlush(city);

        Property riversideProperty = createProperty(
                "PROP-D20-RIVER",
                "Riverside Apartment",
                apartment,
                PropertyPurpose.SALE,
                riversideWard,
                "4500000000",
                "95",
                3,
                2
        );
        Property downtownProperty = createProperty(
                "PROP-D20-DOWN",
                "Downtown Rental House",
                house,
                PropertyPurpose.RENT,
                downtownWard,
                "25000000",
                "80",
                2,
                1
        );

        riverside = createListing(
                "LISTING-D20-RIVER",
                riversideProperty,
                ListingPurpose.SALE,
                ListingStatus.PUBLISHED,
                ListingVisibility.PUBLIC,
                "4500000000",
                8,
                Instant.now().minus(2, ChronoUnit.DAYS)
        );
        downtown = createListing(
                "LISTING-D20-DOWN",
                downtownProperty,
                ListingPurpose.RENT,
                ListingStatus.PUBLISHED,
                ListingVisibility.PUBLIC,
                "25000000",
                20,
                Instant.now().minus(1, ChronoUnit.DAYS)
        );
        draft = createListing(
                "LISTING-D20-DRAFT",
                riversideProperty,
                ListingPurpose.SALE,
                ListingStatus.DRAFT,
                ListingVisibility.PUBLIC,
                "4000000000",
                100,
                null
        );
        internal = createListing(
                "LISTING-D20-INTERNAL",
                riversideProperty,
                ListingPurpose.SALE,
                ListingStatus.PUBLISHED,
                ListingVisibility.INTERNAL,
                "3500000000",
                50,
                Instant.now()
        );
        createListing(
                "LISTING-D20-UNPUBLISHED",
                downtownProperty,
                ListingPurpose.RENT,
                ListingStatus.UNPUBLISHED,
                ListingVisibility.PUBLIC,
                "20000000",
                30,
                Instant.now().minus(3, ChronoUnit.DAYS)
        );
    }

    @Test
    void shouldSearchOnlyPublicPublishedListingsWithFilters() throws Exception {
        mockMvc.perform(get("/api/v1/search/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/search/listings")
                        .queryParam("keyword", "RIVERSIDE")
                        .queryParam("propertyTypeId", apartment.getId().toString())
                        .queryParam("purpose", "SALE")
                        .queryParam("provinceId", city.getId().toString())
                        .queryParam("districtId", centralDistrict.getId().toString())
                        .queryParam("wardId", riversideWard.getId().toString())
                        .queryParam("minPrice", "4000000000")
                        .queryParam("maxPrice", "5000000000")
                        .queryParam("minArea", "90")
                        .queryParam("maxArea", "100")
                        .queryParam("bedrooms", "3")
                        .queryParam("bathrooms", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].code").value(riverside.getCode()))
                .andExpect(jsonPath("$.data.content[0].provinceName").value("Day 20 City"))
                .andExpect(jsonPath("$.data.content[0].wardName").value("Riverside Ward"));
    }

    @Test
    void shouldPaginateSortAndValidatePublicSearch() throws Exception {
        mockMvc.perform(get("/api/v1/search/listings")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .queryParam("sortBy", "price")
                        .queryParam("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content[0].code").value(downtown.getCode()));

        mockMvc.perform(get("/api/v1/search/listings")
                        .queryParam("sortBy", "viewCount")
                        .queryParam("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].code").value(downtown.getCode()))
                .andExpect(jsonPath("$.data.content[1].code").value(riverside.getCode()));

        mockMvc.perform(get("/api/v1/search/listings")
                        .queryParam("minPrice", "100")
                        .queryParam("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/search/listings")
                        .queryParam("sortBy", "status"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldRecordAnonymousAndAuthenticatedListingViews() throws Exception {
        mockMvc.perform(get("/api/v1/search/listings/{slug}", riverside.getSlug())
                        .header("X-Session-Id", "anonymous-session")
                        .header("User-Agent", "Day20 Browser")
                        .header("Referer", "https://example.test/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(9));

        mockMvc.perform(get("/api/v1/search/listings/{slug}", riverside.getSlug())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .header("X-Session-Id", "customer-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(10));

        Listing updated = listingRepository.findById(riverside.getId()).orElseThrow();
        assertThat(updated.getViewCount()).isEqualTo(10);
        List<ListingView> views =
                listingViewRepository.findAllByListingIdOrderByViewedAtDesc(riverside.getId());
        assertThat(views).hasSize(2);
        assertThat(views)
                .extracting(ListingView::getSessionId)
                .containsExactlyInAnyOrder("anonymous-session", "customer-session");
        assertThat(views)
                .extracting(view -> view.getViewer() == null ? null : view.getViewer().getId())
                .containsExactlyInAnyOrder(null, customer.getId());
        assertThat(views)
                .extracting(ListingView::getIpHash)
                .allMatch(hash -> hash != null && hash.length() == 64);

        mockMvc.perform(get("/api/v1/search/listings/{slug}", draft.getSlug()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/search/listings/{slug}", internal.getSlug()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowOnlyCustomersToManagePublishedListingFavorites() throws Exception {
        mockMvc.perform(post("/api/v1/listings/{id}/favorite", riverside.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(riverside.getCode()));

        mockMvc.perform(post("/api/v1/listings/{id}/favorite", riverside.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isCreated());
        assertThat(favoriteRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/listings/favorites")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].code").value(riverside.getCode()));

        riverside.setStatus(ListingStatus.UNPUBLISHED);
        listingRepository.saveAndFlush(riverside);
        mockMvc.perform(get("/api/v1/listings/favorites")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        riverside.setStatus(ListingStatus.PUBLISHED);
        listingRepository.saveAndFlush(riverside);

        mockMvc.perform(post("/api/v1/listings/{id}/favorite", draft.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/listings/{id}/favorite", riverside.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/listings/favorites"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/listings/{id}/favorite", riverside.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk());
        assertThat(favoriteRepository.count()).isZero();
    }

    private Property createProperty(
            String code,
            String name,
            PropertyType type,
            PropertyPurpose purpose,
            Ward ward,
            String price,
            String area,
            int bedrooms,
            int bathrooms
    ) {
        Address address = new Address(ward.getDistrict().getProvince(), code + " Street");
        address.setDistrict(ward.getDistrict());
        address.setWard(ward);
        address.setFullAddress(code + " Street, " + ward.getName());
        Property property = new Property(code, name, type, address, agent, purpose);
        property.setPrice(new BigDecimal(price));
        property.setLandArea(new BigDecimal(area));
        property.setFloorArea(new BigDecimal(area));
        property.setBedrooms(bedrooms);
        property.setBathrooms(bathrooms);
        return propertyRepository.saveAndFlush(property);
    }

    private Listing createListing(
            String code,
            Property property,
            ListingPurpose purpose,
            ListingStatus status,
            ListingVisibility visibility,
            String askingPrice,
            long viewCount,
            Instant publishedAt
    ) {
        Listing listing = new Listing(
                code,
                property,
                agent,
                code + " Title",
                code.toLowerCase(),
                "Public search description for " + property.getName(),
                purpose
        );
        listing.setStatus(status);
        listing.setVisibility(visibility);
        listing.setAskingPrice(new BigDecimal(askingPrice));
        listing.setViewCount(viewCount);
        listing.setPublishedAt(publishedAt);
        return listingRepository.saveAndFlush(listing);
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Day 20 User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
