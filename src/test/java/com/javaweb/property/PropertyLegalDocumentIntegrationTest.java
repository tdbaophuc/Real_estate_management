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
import com.javaweb.property.entity.PropertyLegalDocument;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.LegalDocumentType;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.repository.PropertyLegalDocumentRepository;
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
import java.util.List;
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
        "spring.datasource.url=jdbc:h2:mem:property_legal_document_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.storage.local-root=target/test-storage/property-legal-documents"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropertyLegalDocumentIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";
    private static final Path STORAGE_ROOT = Path.of("target/test-storage/property-legal-documents");
    private static final byte[] PDF_BYTES = "%PDF-1.7 legal document".getBytes();

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
    private PropertyLegalDocumentRepository propertyLegalDocumentRepository;

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
    private User outsiderAgent;
    private User manager;
    private User admin;
    private User customer;
    private Province province;
    private PropertyType propertyType;
    private String creatorToken;
    private String assignedAgentToken;
    private String outsiderAgentToken;
    private String managerToken;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        propertyLegalDocumentRepository.deleteAll();
        fileResourceRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();
        clearStorage();

        creator = createUser("creator-legal@example.test", RoleCode.AGENT);
        assignedAgent = createUser("assigned-legal@example.test", RoleCode.AGENT);
        outsiderAgent = createUser("outsider-legal@example.test", RoleCode.AGENT);
        manager = createUser("manager-legal@example.test", RoleCode.MANAGER);
        admin = createUser("admin-legal@example.test", RoleCode.ADMIN);
        customer = createUser("customer-legal@example.test", RoleCode.CUSTOMER);
        creatorToken = login(creator.getEmail());
        assignedAgentToken = login(assignedAgent.getEmail());
        outsiderAgentToken = login(outsiderAgent.getEmail());
        managerToken = login(manager.getEmail());
        adminToken = login(admin.getEmail());
        customerToken = login(customer.getEmail());

        province = provinceRepository.saveAndFlush(new Province("P-D16", "Day 16 Province"));
        propertyType = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
    }

    @Test
    void shouldUploadListGetUpdateVerifyAndDeleteLegalDocument() throws Exception {
        Property property = createProperty("PROP-D16-FLOW", creator, assignedAgent);
        UploadResult upload = uploadDocument(
                property.getId(),
                "certificate.pdf",
                "PINK_BOOK",
                "CS123456",
                "Department of Natural Resources",
                "2024-01-15",
                null,
                "Original owner copy scanned",
                creatorToken
        );

        mockMvc.perform(get("/api/v1/properties/{propertyId}/legal-documents", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedAgentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(upload.id()));

        mockMvc.perform(get("/api/v1/properties/{propertyId}/legal-documents/{documentId}",
                        property.getId(),
                        upload.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentType").value("PINK_BOOK"))
                .andExpect(jsonPath("$.data.documentNumber").value("CS123456"))
                .andExpect(jsonPath("$.data.verificationStatus").value("UNVERIFIED"));

        PropertyLegalDocument stored = propertyLegalDocumentRepository.findById(upload.id()).orElseThrow();
        Path storedPath = STORAGE_ROOT.resolve(stored.getStorageKey());
        assertThat(Files.exists(storedPath)).isTrue();

        mockMvc.perform(patch("/api/v1/properties/{propertyId}/legal-documents/{documentId}",
                        property.getId(),
                        upload.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "documentType", "LAND_USE_CERTIFICATE",
                                "documentNumber", "CS123456-UPDATED",
                                "issuedBy", "Department of Land",
                                "issuedDate", "2024-02-01",
                                "expiryDate", "2034-02-01",
                                "notes", "Updated notes"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Property legal document updated successfully"))
                .andExpect(jsonPath("$.data.documentType").value("LAND_USE_CERTIFICATE"))
                .andExpect(jsonPath("$.data.documentNumber").value("CS123456-UPDATED"))
                .andExpect(jsonPath("$.data.notes").value("Updated notes"));

        mockMvc.perform(patch("/api/v1/properties/{propertyId}/legal-documents/{documentId}/verify",
                        property.getId(),
                        upload.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "verificationStatus", "VERIFIED",
                                "notes", "Matched owner and address"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Property legal document verification updated successfully"
                ))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));

        mockMvc.perform(delete("/api/v1/properties/{propertyId}/legal-documents/{documentId}",
                        property.getId(),
                        upload.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Property legal document deleted successfully"
                ));

        assertThat(propertyLegalDocumentRepository.findById(upload.id())).isEmpty();
        assertThat(fileResourceRepository.count()).isZero();
        assertThat(Files.exists(storedPath)).isFalse();
    }

    @Test
    void shouldEnforceRoleAndPropertyAuthorizationForLegalDocuments() throws Exception {
        Property property = createProperty("PROP-D16-AUTH", creator, assignedAgent);
        UploadResult upload = uploadDocument(
                property.getId(),
                "auth.pdf",
                "PINK_BOOK",
                "AUTH-001",
                "Department of Natural Resources",
                "2024-01-15",
                null,
                null,
                creatorToken
        );

        mockMvc.perform(get("/api/v1/properties/{propertyId}/legal-documents", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderAgentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/properties/{propertyId}/legal-documents", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/properties/{propertyId}/legal-documents/{documentId}/verify",
                        property.getId(),
                        upload.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "verificationStatus", "VERIFIED",
                                "notes", "Approved"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/v1/properties/{propertyId}/legal-documents/{documentId}",
                        property.getId(),
                        upload.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/properties/{propertyId}/legal-documents/{documentId}",
                        property.getId(),
                        Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRejectInvalidUploadsAndUpdatesForLegalDocuments() throws Exception {
        Property property = createProperty("PROP-D16-VALIDATION", creator, assignedAgent);
        MockMultipartFile invalidPdf = new MockMultipartFile(
                "file",
                "invalid.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "not a pdf".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/properties/{propertyId}/legal-documents", property.getId())
                        .file(invalidPdf)
                        .param("documentType", "PINK_BOOK")
                        .param("documentNumber", "VAL-001")
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_UPLOAD_ERROR"));

        UploadResult upload = uploadDocument(
                property.getId(),
                "valid.pdf",
                "PINK_BOOK",
                "VAL-002",
                "Department of Land",
                "2024-01-15",
                null,
                null,
                creatorToken
        );

        mockMvc.perform(patch("/api/v1/properties/{propertyId}/legal-documents/{documentId}",
                        property.getId(),
                        upload.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "notes", "x".repeat(1200)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/properties/{propertyId}/legal-documents/{documentId}",
                        property.getId(),
                        Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(creatorToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private UploadResult uploadDocument(
            Long propertyId,
            String fileName,
            String documentType,
            String documentNumber,
            String issuedBy,
            String issuedDate,
            String expiryDate,
            String notes,
            String token
    ) throws Exception {
        var request = multipart("/api/v1/properties/{propertyId}/legal-documents", propertyId)
                .file(pdf(fileName))
                .param("documentType", documentType)
                .param("documentNumber", documentNumber)
                .param("issuedBy", issuedBy)
                .param("issuedDate", issuedDate)
                .header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (expiryDate != null) {
            request.param("expiryDate", expiryDate);
        }
        if (notes != null) {
            request.param("notes", notes);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.storageKey").isNotEmpty())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new UploadResult(
                response.at("/data/id").longValue(),
                response.at("/data/storageKey").asText()
        );
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, MediaType.APPLICATION_PDF_VALUE, PDF_BYTES);
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
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Legal User");
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

    private record UploadResult(Long id, String storageKey) {
    }
}
