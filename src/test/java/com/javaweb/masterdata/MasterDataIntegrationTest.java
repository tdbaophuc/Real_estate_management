package com.javaweb.masterdata;

import com.javaweb.lead.entity.LeadSource;
import com.javaweb.lead.repository.LeadSourceRepository;
import com.javaweb.listing.entity.ListingPackage;
import com.javaweb.listing.repository.ListingPackageRepository;
import com.javaweb.property.entity.Amenity;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.enums.AmenityCategory;
import com.javaweb.property.repository.AmenityRepository;
import com.javaweb.property.repository.DistrictRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.property.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:master_data_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterDataIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private ListingPackageRepository listingPackageRepository;

    @Autowired
    private LeadSourceRepository leadSourceRepository;

    private Province activeProvince;
    private District activeDistrict;

    @BeforeEach
    void setUp() {
        seedLocations();
        seedPropertyTypes();
        seedAmenities();
        seedListingPackages();
        seedLeadSources();
    }

    @Test
    void shouldListActiveProvinces() throws Exception {
        mockMvc.perform(get("/api/v1/master-data/provinces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItem("MD_HCM")))
                .andExpect(jsonPath("$.data[*].code").value(not(hasItem("MD_INACTIVE_PROVINCE"))))
                .andExpect(jsonPath("$.data[*].active").value(everyItem(org.hamcrest.Matchers.is(true))));
    }

    @Test
    void shouldListActiveDistrictsByProvince() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/master-data/provinces/{provinceId}/districts",
                        activeProvince.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItem("MD_D1")))
                .andExpect(jsonPath("$.data[*].code").value(not(hasItem("MD_INACTIVE_DISTRICT"))));
    }

    @Test
    void shouldListActiveWardsByDistrict() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/master-data/districts/{districtId}/wards",
                        activeDistrict.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItem("MD_W1")))
                .andExpect(jsonPath("$.data[*].code").value(not(hasItem("MD_INACTIVE_WARD"))));
    }

    @Test
    void shouldListActivePropertyTypes() throws Exception {
        mockMvc.perform(get("/api/v1/master-data/property-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItem("MD_APARTMENT")))
                .andExpect(jsonPath("$.data[*].code").value(not(hasItem("MD_INACTIVE_TYPE"))));
    }

    @Test
    void shouldListActiveAmenitiesAndFilterByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/master-data/amenities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItem("MD_POOL")))
                .andExpect(jsonPath("$.data[*].code").value(hasItem("MD_PARKING")))
                .andExpect(jsonPath("$.data[*].code").value(not(hasItem("MD_INACTIVE_AMENITY"))));

        mockMvc.perform(get("/api/v1/master-data/amenities")
                        .queryParam("category", "LEISURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItem("MD_POOL")))
                .andExpect(jsonPath("$.data[*].category").value(everyItem(org.hamcrest.Matchers.is("LEISURE"))));
    }

    @Test
    void shouldListActiveListingPackages() throws Exception {
        mockMvc.perform(get("/api/v1/master-data/listing-packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItem("MD_PREMIUM")))
                .andExpect(jsonPath("$.data[*].code").value(not(hasItem("MD_INACTIVE_PACKAGE"))));
    }

    @Test
    void shouldListActiveLeadSources() throws Exception {
        mockMvc.perform(get("/api/v1/master-data/lead-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItem("MD_WEBSITE")))
                .andExpect(jsonPath("$.data[*].code").value(not(hasItem("MD_INACTIVE_SOURCE"))));
    }

    private void seedLocations() {
        activeProvince = provinceRepository.findByCode("MD_HCM")
                .orElseGet(() -> provinceRepository.saveAndFlush(new Province(
                        "MD_HCM",
                        "Master Data HCM"
                )));
        Province inactiveProvince = provinceRepository.findByCode("MD_INACTIVE_PROVINCE")
                .orElseGet(() -> {
                    Province province = new Province("MD_INACTIVE_PROVINCE", "Inactive Province");
                    province.setActive(false);
                    return provinceRepository.saveAndFlush(province);
                });
        inactiveProvince.setActive(false);
        provinceRepository.saveAndFlush(inactiveProvince);

        activeDistrict = districtRepository.findByCode("MD_D1")
                .orElseGet(() -> districtRepository.saveAndFlush(new District(
                        activeProvince,
                        "MD_D1",
                        "Master Data District 1"
                )));
        District inactiveDistrict = districtRepository.findByCode("MD_INACTIVE_DISTRICT")
                .orElseGet(() -> {
                    District district = new District(
                            activeProvince,
                            "MD_INACTIVE_DISTRICT",
                            "Inactive District"
                    );
                    district.setActive(false);
                    return districtRepository.saveAndFlush(district);
                });
        inactiveDistrict.setActive(false);
        districtRepository.saveAndFlush(inactiveDistrict);

        wardRepository.findByCode("MD_W1")
                .orElseGet(() -> wardRepository.saveAndFlush(new Ward(
                        activeDistrict,
                        "MD_W1",
                        "Master Data Ward 1"
                )));
        Ward inactiveWard = wardRepository.findByCode("MD_INACTIVE_WARD")
                .orElseGet(() -> {
                    Ward ward = new Ward(activeDistrict, "MD_INACTIVE_WARD", "Inactive Ward");
                    ward.setActive(false);
                    return wardRepository.saveAndFlush(ward);
                });
        inactiveWard.setActive(false);
        wardRepository.saveAndFlush(inactiveWard);
    }

    private void seedPropertyTypes() {
        propertyTypeRepository.findByCode("MD_APARTMENT")
                .orElseGet(() -> propertyTypeRepository.saveAndFlush(new PropertyType(
                        "MD_APARTMENT",
                        "Master Data Apartment"
                )));
        PropertyType inactive = propertyTypeRepository.findByCode("MD_INACTIVE_TYPE")
                .orElseGet(() -> {
                    PropertyType type = new PropertyType("MD_INACTIVE_TYPE", "Inactive Type");
                    type.setActive(false);
                    return propertyTypeRepository.saveAndFlush(type);
                });
        inactive.setActive(false);
        propertyTypeRepository.saveAndFlush(inactive);
    }

    private void seedAmenities() {
        amenityRepository.findByCode("MD_POOL")
                .orElseGet(() -> amenityRepository.saveAndFlush(new Amenity(
                        "MD_POOL",
                        "Master Data Pool",
                        AmenityCategory.LEISURE
                )));
        amenityRepository.findByCode("MD_PARKING")
                .orElseGet(() -> amenityRepository.saveAndFlush(new Amenity(
                        "MD_PARKING",
                        "Master Data Parking",
                        AmenityCategory.ACCESS
                )));
        Amenity inactive = amenityRepository.findByCode("MD_INACTIVE_AMENITY")
                .orElseGet(() -> {
                    Amenity amenity = new Amenity(
                            "MD_INACTIVE_AMENITY",
                            "Inactive Amenity",
                            AmenityCategory.FEATURE
                    );
                    amenity.setActive(false);
                    return amenityRepository.saveAndFlush(amenity);
                });
        inactive.setActive(false);
        amenityRepository.saveAndFlush(inactive);
    }

    private void seedListingPackages() {
        listingPackageRepository.findByCode("MD_PREMIUM")
                .orElseGet(() -> {
                    ListingPackage item = new ListingPackage("MD_PREMIUM", "Master Data Premium", 30);
                    item.setPrice(BigDecimal.valueOf(100000));
                    item.setPriorityLevel(10);
                    return listingPackageRepository.saveAndFlush(item);
                });
        ListingPackage inactive = listingPackageRepository.findByCode("MD_INACTIVE_PACKAGE")
                .orElseGet(() -> {
                    ListingPackage item = new ListingPackage(
                            "MD_INACTIVE_PACKAGE",
                            "Inactive Package",
                            7
                    );
                    item.setActive(false);
                    return listingPackageRepository.saveAndFlush(item);
                });
        inactive.setActive(false);
        listingPackageRepository.saveAndFlush(inactive);
    }

    private void seedLeadSources() {
        leadSourceRepository.findByCodeAndActiveTrue("MD_WEBSITE")
                .orElseGet(() -> leadSourceRepository.saveAndFlush(new LeadSource(
                        "MD_WEBSITE",
                        "Master Data Website"
                )));
        LeadSource inactive = leadSourceRepository.findByCode("MD_INACTIVE_SOURCE")
                .orElseGet(() -> {
                    LeadSource source = new LeadSource("MD_INACTIVE_SOURCE", "Inactive Source");
                    source.setActive(false);
                    return leadSourceRepository.saveAndFlush(source);
                });
        inactive.setActive(false);
        leadSourceRepository.saveAndFlush(inactive);
    }
}
