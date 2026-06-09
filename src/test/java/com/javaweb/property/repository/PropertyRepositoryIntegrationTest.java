package com.javaweb.property.repository;

import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Amenity;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyImage;
import com.javaweb.property.entity.PropertyLegalDocument;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.enums.DocumentVerificationStatus;
import com.javaweb.property.enums.FurnitureStatus;
import com.javaweb.property.enums.LegalDocumentType;
import com.javaweb.property.enums.PropertyDirection;
import com.javaweb.property.enums.PropertyLegalStatus;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PropertyRepositoryIntegrationTest {

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyAmenityRepository propertyAmenityRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    @Autowired
    private PropertyLegalDocumentRepository legalDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndQueryLocationHierarchy() {
        Province province = new Province("79", "Ho Chi Minh City");
        province.setAdministrativeType("MUNICIPALITY");
        District district = new District(province, "760", "District 1");
        district.setAdministrativeType("URBAN_DISTRICT");
        Ward ward = new Ward(district, "26734", "Ben Nghe");
        ward.setAdministrativeType("WARD");
        province.addDistrict(district);
        district.addWard(ward);

        provinceRepository.saveAndFlush(province);

        assertThat(provinceRepository.findByCode("79")).contains(province);
        assertThat(districtRepository.findAllByProvinceIdAndActiveTrueOrderByNameAsc(province.getId()))
                .containsExactly(district);
        assertThat(wardRepository.findAllByDistrictIdAndActiveTrueOrderByNameAsc(district.getId()))
                .containsExactly(ward);
        assertThat(province.getCreatedAt()).isNotNull();
        assertThat(province.getUpdatedAt()).isNotNull();
        assertThat(district.getCreatedAt()).isNotNull();
        assertThat(ward.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldPersistPropertyAggregateAndQueryIndexedFields() {
        Province province = new Province("01", "Ha Noi");
        District district = new District(province, "001", "Ba Dinh");
        Ward ward = new Ward(district, "00001", "Phuc Xa");
        province.addDistrict(district);
        district.addWard(ward);
        provinceRepository.saveAndFlush(province);

        User agent = new User("property-agent@example.test", "bcrypt-hash", "Property Agent");
        agent.setStatus(UserStatus.ACTIVE);
        agent = userRepository.saveAndFlush(agent);

        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
        Amenity parking = amenityRepository.findByCode("PARKING").orElseThrow();

        Address address = new Address(province, "12 Test Street");
        address.setDistrict(district);
        address.setWard(ward);
        address.setFullAddress("12 Test Street, Phuc Xa, Ba Dinh, Ha Noi");
        address.setLatitude(new BigDecimal("21.0401234"));
        address.setLongitude(new BigDecimal("105.8401234"));

        Property property = new Property(
                "PROP-TEST-001",
                "Test apartment",
                propertyType,
                address,
                agent,
                PropertyPurpose.SALE
        );
        property.setOwner(agent);
        property.setAssignedAgent(agent);
        property.setStatus(PropertyStatus.AVAILABLE);
        property.setPrice(new BigDecimal("5000000000.00"));
        property.setLandArea(new BigDecimal("90.50"));
        property.setFloorArea(new BigDecimal("82.00"));
        property.setBedrooms(3);
        property.setBathrooms(2);
        property.setFloors(1);
        property.setDirection(PropertyDirection.SOUTHEAST);
        property.setLegalStatus(PropertyLegalStatus.PINK_BOOK);
        property.setFurnitureStatus(FurnitureStatus.FULLY_FURNISHED);
        property.setAvailableFrom(LocalDate.of(2026, 7, 1));

        PropertyImage image = new PropertyImage(agent, "https://example.test/property.jpg");
        image.setStorageKey("properties/test/property.jpg");
        image.setCoverImage(true);
        image.setDisplayOrder(1);
        property.addImage(image);

        PropertyLegalDocument document = new PropertyLegalDocument(agent, LegalDocumentType.PINK_BOOK);
        document.setDocumentNumber("PB-TEST-001");
        document.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        document.setStorageKey("properties/test/pink-book.pdf");
        property.addLegalDocument(document);
        property.addAmenity(parking, "One basement parking slot");

        property = propertyRepository.saveAndFlush(property);
        entityManager.clear();

        Property lazyProperty = propertyRepository.findById(property.getId()).orElseThrow();
        PersistenceUnitUtil persistence = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(persistence.isLoaded(lazyProperty, "propertyType")).isFalse();
        assertThat(persistence.isLoaded(lazyProperty, "address")).isFalse();
        assertThat(persistence.isLoaded(lazyProperty, "images")).isFalse();
        assertThat(persistence.isLoaded(lazyProperty, "legalDocuments")).isFalse();
        assertThat(persistence.isLoaded(lazyProperty, "amenities")).isFalse();

        Property loaded = propertyRepository.findWithCoreDetailsById(property.getId()).orElseThrow();
        assertThat(loaded.getPurpose()).isEqualTo(PropertyPurpose.SALE);
        assertThat(loaded.getStatus()).isEqualTo(PropertyStatus.AVAILABLE);
        assertThat(loaded.getDirection()).isEqualTo(PropertyDirection.SOUTHEAST);
        assertThat(loaded.getLegalStatus()).isEqualTo(PropertyLegalStatus.PINK_BOOK);
        assertThat(loaded.getFurnitureStatus()).isEqualTo(FurnitureStatus.FULLY_FURNISHED);
        assertThat(loaded.getPropertyType().getCode()).isEqualTo("APARTMENT");
        assertThat(loaded.getAddress().getWard().getCode()).isEqualTo("00001");
        assertThat(loaded.getCreatedBy().getEmail()).isEqualTo("property-agent@example.test");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();

        assertThat(propertyRepository.findAllByPurposeAndStatusAndDeletedAtIsNull(
                PropertyPurpose.SALE,
                PropertyStatus.AVAILABLE,
                PageRequest.of(0, 10)
        )).containsExactly(loaded);
        assertThat(propertyRepository.findAllByPropertyTypeIdAndStatusAndDeletedAtIsNull(
                propertyType.getId(),
                PropertyStatus.AVAILABLE,
                PageRequest.of(0, 10)
        )).containsExactly(loaded);
        assertThat(addressRepository.findAllByProvinceIdAndDistrictIdAndWardId(
                province.getId(),
                district.getId(),
                ward.getId()
        )).hasSize(1);

        assertThat(propertyAmenityRepository.findAllByPropertyId(property.getId()))
                .singleElement()
                .satisfies(savedAmenity -> {
                    assertThat(savedAmenity.getAmenity().getCode()).isEqualTo("PARKING");
                    assertThat(savedAmenity.getCreatedAt()).isNotNull();
                });

        assertThat(propertyImageRepository
                .findAllByPropertyIdOrderByCoverImageDescDisplayOrderAsc(property.getId()))
                .singleElement()
                .satisfies(savedImage -> {
                    assertThat(savedImage.isCoverImage()).isTrue();
                    assertThat(savedImage.getCreatedAt()).isNotNull();
                });
        assertThat(propertyImageRepository.findFirstByPropertyIdAndCoverImageTrue(property.getId()))
                .isPresent();

        assertThat(legalDocumentRepository.findAllByPropertyIdAndDocumentType(
                property.getId(),
                LegalDocumentType.PINK_BOOK
        )).singleElement().satisfies(savedDocument -> {
            assertThat(savedDocument.getVerificationStatus())
                    .isEqualTo(DocumentVerificationStatus.VERIFIED);
            assertThat(savedDocument.getCreatedAt()).isNotNull();
            assertThat(savedDocument.getUpdatedAt()).isNotNull();
        });
    }
}
