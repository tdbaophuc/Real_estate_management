package com.javaweb.listing.repository;

import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.entity.ListingPackage;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ListingRepositoryIntegrationTest {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingPackageRepository listingPackageRepository;

    @Autowired
    private ListingStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private ListingViewRepository listingViewRepository;

    @Autowired
    private ListingFavoriteRepository favoriteRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistListingAggregateAndQueryMappedRelationships() {
        User agent = new User("listing-agent@example.test", "bcrypt-hash", "Listing Agent");
        agent.setStatus(UserStatus.ACTIVE);
        agent = userRepository.saveAndFlush(agent);

        User customer = new User("listing-customer@example.test", "bcrypt-hash", "Listing Customer");
        customer.setStatus(UserStatus.ACTIVE);
        customer = userRepository.saveAndFlush(customer);

        Province province = provinceRepository.saveAndFlush(new Province("L01", "Listing Province"));
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
        Address address = new Address(province, "17 Listing Street");
        address.setFullAddress("17 Listing Street, Listing Province");

        Property property = new Property(
                "PROP-LISTING-017",
                "Listing source property",
                propertyType,
                address,
                agent,
                PropertyPurpose.SALE
        );
        property.setPrice(new BigDecimal("5000000000.00"));
        property = propertyRepository.saveAndFlush(property);

        ListingPackage premiumPackage = new ListingPackage("PREMIUM-30", "Premium 30 days", 30);
        premiumPackage.setPrice(new BigDecimal("500000.00"));
        premiumPackage.setFeatured(true);
        premiumPackage.setPriorityLevel(100);
        premiumPackage = listingPackageRepository.saveAndFlush(premiumPackage);

        Listing listing = new Listing(
                "LISTING-017",
                property,
                agent,
                "Apartment for sale",
                "apartment-for-sale-listing-017",
                "Public listing description",
                ListingPurpose.SALE
        );
        listing.setListingPackage(premiumPackage);
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setVisibility(ListingVisibility.PUBLIC);
        listing.setAskingPrice(new BigDecimal("5200000000.00"));
        listing.setPublishedAt(Instant.parse("2026-06-10T05:00:00Z"));
        listing.setExpiresAt(Instant.parse("2026-07-10T05:00:00Z"));
        listing.setFeaturedUntil(Instant.parse("2026-06-20T05:00:00Z"));
        listing.setViewCount(1);
        listing.addStatusHistory(
                ListingStatus.DRAFT,
                ListingStatus.PUBLISHED,
                agent,
                "Approved and published"
        );
        listing.addView(customer, "session-listing-017");
        listing.addFavorite(customer);

        listing = listingRepository.saveAndFlush(listing);
        Long listingId = listing.getId();
        Long propertyId = property.getId();
        Long agentId = agent.getId();
        Long customerId = customer.getId();
        entityManager.clear();

        Listing lazyListing = listingRepository.findById(listingId).orElseThrow();
        PersistenceUnitUtil persistence = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(persistence.isLoaded(lazyListing, "property")).isFalse();
        assertThat(persistence.isLoaded(lazyListing, "createdBy")).isFalse();
        assertThat(persistence.isLoaded(lazyListing, "listingPackage")).isFalse();
        assertThat(persistence.isLoaded(lazyListing, "statusHistories")).isFalse();
        assertThat(persistence.isLoaded(lazyListing, "views")).isFalse();
        assertThat(persistence.isLoaded(lazyListing, "favorites")).isFalse();

        Listing loaded = listingRepository.findWithCoreDetailsById(listingId).orElseThrow();
        assertThat(loaded.getProperty().getId()).isEqualTo(propertyId);
        assertThat(loaded.getProperty().getPropertyType().getCode()).isEqualTo("APARTMENT");
        assertThat(loaded.getCreatedBy().getEmail()).isEqualTo("listing-agent@example.test");
        assertThat(loaded.getListingPackage().getCode()).isEqualTo("PREMIUM-30");
        assertThat(loaded.getPurpose()).isEqualTo(ListingPurpose.SALE);
        assertThat(loaded.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(loaded.getVisibility()).isEqualTo(ListingVisibility.PUBLIC);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();

        assertThat(listingRepository.findByCode("LISTING-017")).contains(loaded);
        assertThat(listingRepository.findBySlugAndDeletedAtIsNull("apartment-for-sale-listing-017"))
                .contains(loaded);
        assertThat(listingRepository.findAllByPropertyIdAndDeletedAtIsNullOrderByCreatedAtDesc(propertyId))
                .containsExactly(loaded);
        assertThat(listingRepository.findAllByCreatedByIdAndDeletedAtIsNull(
                agentId,
                PageRequest.of(0, 10)
        )).containsExactly(loaded);
        assertThat(listingRepository.findAllByStatusAndVisibilityAndDeletedAtIsNull(
                ListingStatus.PUBLISHED,
                ListingVisibility.PUBLIC,
                PageRequest.of(0, 10)
        )).containsExactly(loaded);

        assertThat(listingPackageRepository.findByCode("PREMIUM-30"))
                .get()
                .extracting(ListingPackage::getId)
                .isEqualTo(premiumPackage.getId());
        assertThat(listingPackageRepository.findAllByActiveTrueOrderByPriorityLevelDescNameAsc())
                .extracting(ListingPackage::getCode)
                .contains("PREMIUM-30");
        assertThat(statusHistoryRepository.findAllByListingIdOrderByCreatedAtDesc(listingId))
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getFromStatus()).isEqualTo(ListingStatus.DRAFT);
                    assertThat(history.getToStatus()).isEqualTo(ListingStatus.PUBLISHED);
                    assertThat(history.getChangedBy().getId()).isEqualTo(agentId);
                    assertThat(history.getCreatedAt()).isNotNull();
                });
        assertThat(listingViewRepository.countByListingId(listingId)).isEqualTo(1);
        assertThat(listingViewRepository.findAllByListingIdOrderByViewedAtDesc(listingId))
                .singleElement()
                .satisfies(view -> {
                    assertThat(view.getViewer().getId()).isEqualTo(customerId);
                    assertThat(view.getSessionId()).isEqualTo("session-listing-017");
                    assertThat(view.getViewedAt()).isNotNull();
                });
        assertThat(favoriteRepository.existsByListingIdAndUserId(listingId, customerId)).isTrue();
        assertThat(favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(
                customerId,
                PageRequest.of(0, 10)
        )).hasSize(1);
        assertThat(favoriteRepository.deleteByListingIdAndUserId(listingId, customerId)).isEqualTo(1);
        assertThat(favoriteRepository.existsByListingIdAndUserId(listingId, customerId)).isFalse();
    }
}
