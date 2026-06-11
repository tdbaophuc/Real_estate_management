package com.javaweb.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyImage;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;
import com.javaweb.property.repository.PropertyImageRepository;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.storage.repository.FileResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:property_image_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.storage.local-root=target/test-storage/property-images"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropertyImageIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";
    private static final Path STORAGE_ROOT = Path.of("target/test-storage/property-images");
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x00
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    @Autowired
    private FileResourceRepository fileResourceRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User creator;
    private User assignedAgent;
    private User manager;
    private Province province;
    private PropertyType propertyType;
    private String creatorToken;
    private String assignedAgentToken;
    private String managerToken;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        propertyImageRepository.deleteAll();
        fileResourceRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();
        clearStorage();

        creator = createUser("creator-images@example.test", RoleCode.AGENT);
        assignedAgent = createUser("assigned-images@example.test", RoleCode.AGENT);
        manager = createUser("manager-images@example.test", RoleCode.MANAGER);
        User admin = createUser("admin-images@example.test", RoleCode.ADMIN);
        User customer = createUser("customer-images@example.test", RoleCode.CUSTOMER);
        creatorToken = login(creator.getEmail());
        assignedAgentToken = login(assignedAgent.getEmail());
        managerToken = login(manager.getEmail());
        adminToken = login(admin.getEmail());
        customerToken = login(customer.getEmail());

        province = provinceRepository.saveAndFlush(new Province("P-D15", "Day 15 Province"));
        propertyType = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
    }

    @Test
    void shouldUploadListAndChangeCoverImage() throws Exception {
        Property property = createProperty("PROP-D15-FLOW", creator, assignedAgent);
        Long firstId = uploadImage(
                property.getId(),
                "first.png",
                "First image",
                20,
                creatorToken
        );
        Long secondId = uploadImage(
                property.getId(),
                "second.png",
                "Second image",
                10,
                creatorToken
        );

        mockMvc.perform(get("/api/v1/properties/{id}/images", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedAgentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(firstId))
                .andExpect(jsonPath("$.data[0].coverImage").value(true))
                .andExpect(jsonPath("$.data[1].id").value(secondId))
                .andExpect(jsonPath("$.data[1].coverImage").value(false));

        mockMvc.perform(patch(
                        "/api/v1/properties/{propertyId}/cover-image/{imageId}",
                        property.getId(),
                        secondId
                )
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Property cover image updated successfully"
                ))
                .andExpect(jsonPath("$.data.id").value(secondId))
                .andExpect(jsonPath("$.data.coverImage").value(true));

        assertThat(propertyImageRepository.findFirstByPropertyIdAndCoverImageTrue(property.getId()))
                .get()
                .extracting(PropertyImage::getId)
                .isEqualTo(secondId);
    }

    @Test
    void deletingCoverShouldRemoveStoredFileAndPromoteNextImage() throws Exception {
        Property property = createProperty("PROP-D15-DELETE", creator, assignedAgent);
        Long firstId = uploadImage(
                property.getId(),
                "first.png",
                null,
                20,
                creatorToken
        );
        Long secondId = uploadImage(
                property.getId(),
                "second.png",
                null,
                10,
                creatorToken
        );
        PropertyImage first = propertyImageRepository.findByIdAndPropertyId(
                firstId,
                property.getId()
        ).orElseThrow();
        Path storedPath = STORAGE_ROOT.resolve(first.getStorageKey());
        Long fileResourceId = first.getFileResource().getId();
        assertThat(Files.exists(storedPath)).isTrue();

        mockMvc.perform(delete(
                        "/api/v1/properties/{propertyId}/images/{imageId}",
                        property.getId(),
                        firstId
                )
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Property image deleted successfully"
                ));

        assertThat(propertyImageRepository.findById(firstId)).isEmpty();
        assertThat(fileResourceRepository.findById(fileResourceId)).isEmpty();
        assertThat(Files.exists(storedPath)).isFalse();
        assertThat(propertyImageRepository.findFirstByPropertyIdAndCoverImageTrue(property.getId()))
                .get()
                .extracting(PropertyImage::getId)
                .isEqualTo(secondId);
    }

    @Test
    void shouldEnforceImageOwnershipAndEndpointRoles() throws Exception {
        Property property = createProperty("PROP-D15-AUTH", creator, assignedAgent);
        Long imageId = uploadImage(
                property.getId(),
                "owner.png",
                null,
                0,
                creatorToken
        );

        mockMvc.perform(multipart("/api/v1/properties/{id}/images", property.getId())
                        .file(png("assigned.png"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedAgentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(
                        "/api/v1/properties/{propertyId}/images/{imageId}",
                        property.getId(),
                        imageId
                )
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedAgentToken)))
                .andExpect(status().isForbidden());

        Long managerImageId = uploadImage(
                property.getId(),
                "manager.png",
                null,
                1,
                managerToken
        );
        mockMvc.perform(patch(
                        "/api/v1/properties/{propertyId}/cover-image/{imageId}",
                        property.getId(),
                        managerImageId
                )
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/properties/{id}/images", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/properties/{id}/images", property.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectDeletedPropertyAndImageFromAnotherProperty() throws Exception {
        Property first = createProperty("PROP-D15-FIRST", creator, assignedAgent);
        Property second = createProperty("PROP-D15-SECOND", creator, assignedAgent);
        Long imageId = uploadImage(first.getId(), "first.png", null, 0, creatorToken);

        mockMvc.perform(patch(
                        "/api/v1/properties/{propertyId}/cover-image/{imageId}",
                        second.getId(),
                        imageId
                )
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        second.setStatus(PropertyStatus.DELETED);
        second.setDeletedAt(Instant.now());
        propertyRepository.saveAndFlush(second);
        mockMvc.perform(multipart("/api/v1/properties/{id}/images", second.getId())
                        .file(png("deleted.png"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDocumentUploadForPropertyImages() throws Exception {
        Property property = createProperty("PROP-D15-PDF", creator, assignedAgent);
        MockMultipartFile document = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 test".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/properties/{id}/images", property.getId())
                        .file(document)
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_UPLOAD_ERROR"));

        assertThat(propertyImageRepository.count()).isZero();
        assertThat(fileResourceRepository.count()).isZero();
    }

    private Long uploadImage(
            Long propertyId,
            String fileName,
            String altText,
            int displayOrder,
            String token
    ) throws Exception {
        var request = multipart("/api/v1/properties/{id}/images", propertyId)
                .file(png(fileName))
                .param("displayOrder", Integer.toString(displayOrder))
                .header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (altText != null) {
            request.param("altText", altText);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageUrl").isNotEmpty())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/id").longValue();
    }

    private MockMultipartFile png(String name) {
        return new MockMultipartFile("file", name, MediaType.IMAGE_PNG_VALUE, PNG_BYTES);
    }

    private Property createProperty(String code, User createdBy, User assignedTo) {
        Address address = new Address(province, code + " Street");
        Property property = new Property(
                code,
                code + " Name",
                propertyType,
                address,
                createdBy,
                PropertyPurpose.SALE
        );
        property.setAssignedAgent(assignedTo);
        return propertyRepository.saveAndFlush(property);
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Image User");
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

    private void clearStorage() throws Exception {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }
        try (var paths = Files.walk(STORAGE_ROOT)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(STORAGE_ROOT))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
        Files.createDirectories(STORAGE_ROOT.resolve("public"));
        Files.createDirectories(STORAGE_ROOT.resolve("private"));
    }
}
