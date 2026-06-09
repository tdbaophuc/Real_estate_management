package com.javaweb.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.storage.entity.FileResource;
import com.javaweb.storage.enums.FileAccessLevel;
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
import java.util.Comparator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:file_upload_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.storage.local-root=target/test-storage/file-upload"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileUploadIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";
    private static final Path STORAGE_ROOT = Path.of("target/test-storage/file-upload");
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
    private FileResourceRepository fileResourceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User agent;
    private String agentToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        fileResourceRepository.deleteAll();
        userRepository.deleteAll();
        clearStorage();

        agent = createUser("agent-upload@example.test", RoleCode.AGENT);
        User customer = createUser("customer-upload@example.test", RoleCode.CUSTOMER);
        agentToken = login(agent.getEmail());
        customerToken = login(customer.getEmail());
    }

    @Test
    void agentShouldUploadPublicFileAndPersistMetadata() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "file",
                "listing.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_BYTES
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(image)
                        .param("accessLevel", "PUBLIC")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpect(jsonPath("$.data.originalFileName").value("listing.png"))
                .andExpect(jsonPath("$.data.contentType").value(MediaType.IMAGE_PNG_VALUE))
                .andExpect(jsonPath("$.data.fileSize").value(PNG_BYTES.length))
                .andExpect(jsonPath("$.data.storageProvider").value("LOCAL"))
                .andExpect(jsonPath("$.data.accessLevel").value("PUBLIC"))
                .andExpect(jsonPath("$.data.uploadedById").value(agent.getId()))
                .andExpect(jsonPath("$.data.publicUrl").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        FileResource resource = fileResourceRepository.findById(data.path("id").longValue())
                .orElseThrow();
        assertThat(resource.getChecksumSha256()).hasSize(64);
        assertThat(resource.getStorageKey()).startsWith("public/");
        assertThat(Files.exists(STORAGE_ROOT.resolve(resource.getStorageKey()))).isTrue();

        mockMvc.perform(get(data.path("publicUrl").asText()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    void privateUploadShouldNotExposePublicUrl() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "file",
                "contract.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 test".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(document)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessLevel").value("PRIVATE"))
                .andExpect(jsonPath("$.data.publicUrl").doesNotExist())
                .andReturn();

        Long id = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/id")
                .longValue();
        FileResource resource = fileResourceRepository.findById(id).orElseThrow();
        assertThat(resource.getStorageKey()).startsWith("private/");
        assertThat(Files.exists(STORAGE_ROOT.resolve(resource.getStorageKey()))).isTrue();
    }

    @Test
    void uploadShouldRequireAuthorizedInternalRole() throws Exception {
        MockMultipartFile image = png("listing.png", PNG_BYTES);

        mockMvc.perform(multipart("/api/v1/files/upload").file(image))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(image)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void uploadShouldRejectInvalidFiles() throws Exception {
        assertRejected(new MockMultipartFile(
                "file",
                "notes.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "text".getBytes()
        ));
        assertRejected(new MockMultipartFile(
                "file",
                "listing.jpg",
                MediaType.IMAGE_PNG_VALUE,
                PNG_BYTES
        ));
        assertRejected(png("fake.png", "not a png".getBytes()));
        assertRejected(png("empty.png", new byte[0]));
        assertRejected(png("../listing.png", PNG_BYTES));

        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PNG_BYTES, 0, oversized, 0, PNG_BYTES.length);
        assertRejected(png("large.png", oversized));

        assertThat(fileResourceRepository.count()).isZero();
    }

    private void assertRejected(MockMultipartFile file) throws Exception {
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_UPLOAD_ERROR"));
    }

    private MockMultipartFile png(String name, byte[] content) {
        return new MockMultipartFile("file", name, MediaType.IMAGE_PNG_VALUE, content);
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Upload User");
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
