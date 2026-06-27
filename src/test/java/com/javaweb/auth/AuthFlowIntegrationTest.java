package com.javaweb.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RefreshTokenRepository;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.storage.repository.FileResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_flow_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {
    private static final String EMAIL = "jwt-user@example.test";
    private static final String PASSWORD = "StrongPassword123!";
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private FileResourceRepository fileResourceRepository;

    @BeforeEach
    void setUpUser() {
        refreshTokenRepository.deleteAll();
        userRepository.findByEmailIgnoreCase(EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase("other-session@example.test").ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase("duplicate-phone@example.test").ifPresent(userRepository::delete);

        Role manager = roleRepository.findByCode(RoleCode.MANAGER).orElseThrow();
        User user = new User(EMAIL, passwordEncoder.encode(PASSWORD), "JWT Test User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(manager);
        userRepository.saveAndFlush(user);
    }

    @Test
    void shouldLoginAndReturnCurrentUserWithJwt() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "JWT-USER@example.test",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshExpiresIn").value(2592000))
                .andExpect(jsonPath("$.data.user.email").value(EMAIL))
                .andExpect(jsonPath("$.data.user.roles[0]").value("MANAGER"))
                .andExpect(jsonPath("$.data.user.permissions").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(loginResponse);
        String accessToken = body.at("/data/accessToken").asText();
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.fullName").value("JWT Test User"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.roles[0]").value("MANAGER"))
                .andExpect(jsonPath("$.data.permissions").isArray());

        User updatedUser = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(updatedUser.getLastLoginAt()).isNotNull();
    }

    @Test
    void shouldRejectInvalidCredentialsAndInvalidLoginRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "jwt-user@example.test",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldRequireValidBearerTokenForCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRegisterCustomerWithHashedPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": " New.Customer@Example.Test ",
                                  "password": "Registration123!",
                                  "fullName": " New Customer ",
                                  "phone": "+84901234567"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("new.customer@example.test"))
                .andExpect(jsonPath("$.data.fullName").value("New Customer"))
                .andExpect(jsonPath("$.data.phone").value("+84901234567"))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"));

        User registered = userRepository
                .findWithRolesByEmailIgnoreCase("new.customer@example.test")
                .orElseThrow();
        assertThat(registered.getPasswordHash()).isNotEqualTo("Registration123!");
        assertThat(passwordEncoder.matches("Registration123!", registered.getPasswordHash()))
                .isTrue();
        assertThat(registered.getRoles())
                .extracting(Role::getCode)
                .containsExactly(RoleCode.CUSTOMER);
    }

    @Test
    void shouldRejectDuplicateEmailAndPhoneAsBusinessErrors() throws Exception {
        registerCustomer("duplicate@example.test", "+84901111111");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "DUPLICATE@example.test",
                                  "password": "Registration123!",
                                  "fullName": "Duplicate Email",
                                  "phone": "+84902222222"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "another@example.test",
                                  "password": "Registration123!",
                                  "fullName": "Duplicate Phone",
                                  "phone": "+84901111111"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("Phone is already registered"));
    }

    @Test
    void shouldValidateRegistrationRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid",
                                  "password": "short",
                                  "fullName": "",
                                  "phone": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldValidateRefreshAndLogoutRequests() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRotateRefreshTokenAndRevokeItOnLogout() throws Exception {
        String loginBody = login();
        String firstRefreshToken = objectMapper.readTree(loginBody)
                .at("/data/refreshToken")
                .asText();

        assertThat(refreshTokenRepository.findByTokenHash(firstRefreshToken)).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(sha256(firstRefreshToken)))
                .isPresent()
                .get()
                .extracting(token -> token.getRevokedAt())
                .isNull();

        String refreshBody = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", firstRefreshToken)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondRefreshToken = objectMapper.readTree(refreshBody)
                .at("/data/refreshToken")
                .asText();
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(refreshTokenRepository.findByTokenHash(sha256(firstRefreshToken)))
                .isPresent()
                .get()
                .extracting(token -> token.getRevokedAt())
                .isNotNull();

        refreshExpectingUnauthorized(firstRefreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", secondRefreshToken)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        refreshExpectingUnauthorized(secondRefreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", secondRefreshToken)
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectLoginForInactiveUser() throws Exception {
        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "jwt-user@example.test",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldUpdateCurrentProfileWithoutChangingProtectedFields() throws Exception {
        createActiveUser("duplicate-phone@example.test", "+84909999999", RoleCode.CUSTOMER);
        String accessToken = accessToken(login());

        mockMvc.perform(patch("/api/v1/auth/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": " Updated Name ",
                                  "phone": "+84901230000",
                                  "email": "attacker@example.test",
                                  "status": "INACTIVE",
                                  "roles": ["ADMIN"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.data.phone").value("+84901230000"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.roles[0]").value("MANAGER"));

        User updated = userRepository.findWithRolesByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Updated Name");
        assertThat(updated.getEmail()).isEqualTo(EMAIL);
        assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(updated.getRoles()).extracting(Role::getCode).containsExactly(RoleCode.MANAGER);

        mockMvc.perform(patch("/api/v1/auth/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Updated Name",
                                  "phone": "+84909999999"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void shouldChangePasswordAndRevokeRefreshTokens() throws Exception {
        String firstLogin = login();
        String accessToken = accessToken(firstLogin);
        String firstRefreshToken = objectMapper.readTree(firstLogin).at("/data/refreshToken").asText();
        String secondRefreshToken = objectMapper.readTree(login()).at("/data/refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/me/change-password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "StrongPassword123!",
                                  "newPassword": "NewStrongPassword123!",
                                  "confirmPassword": "NewStrongPassword123!"
                                }
                                """))
                .andExpect(status().isOk());

        refreshExpectingUnauthorized(firstRefreshToken);
        refreshExpectingUnauthorized(secondRefreshToken);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", EMAIL,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", EMAIL,
                                "password", "NewStrongPassword123!"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldValidateChangePasswordRequest() throws Exception {
        String accessToken = accessToken(login());

        mockMvc.perform(post("/api/v1/auth/me/change-password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "wrong",
                                  "newPassword": "NewStrongPassword123!",
                                  "confirmPassword": "NewStrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/auth/me/change-password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "StrongPassword123!",
                                  "newPassword": "weakpasswordonly",
                                  "confirmPassword": "weakpasswordonly"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldUploadAndDeleteAvatarThroughFileResourceStorage() throws Exception {
        String accessToken = accessToken(login());
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_BYTES
        );

        mockMvc.perform(multipart("/api/v1/auth/me/avatar")
                        .file(avatar)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").isNotEmpty());

        User withAvatar = userRepository.findWithRolesByEmailIgnoreCase(EMAIL).orElseThrow();
        Long avatarFileId = withAvatar.getAvatarFileResource().getId();
        assertThat(withAvatar.getAvatarFileResource().getStorageProvider().name()).isEqualTo("LOCAL");
        assertThat(fileResourceRepository.findById(avatarFileId)).isPresent();

        mockMvc.perform(delete("/api/v1/auth/me/avatar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").doesNotExist());

        User withoutAvatar = userRepository.findWithRolesByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(withoutAvatar.getAvatarFileResource()).isNull();
        assertThat(fileResourceRepository.findById(avatarFileId)).isEmpty();
    }

    @Test
    void shouldListAndRevokeOnlyCurrentUserSessions() throws Exception {
        String loginBody = login();
        String accessToken = accessToken(loginBody);
        String refreshToken = objectMapper.readTree(loginBody).at("/data/refreshToken").asText();
        User current = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        Long currentSessionId = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(current.getId())
                .getFirst()
                .getId();

        User other = createActiveUser("other-session@example.test", "+84908888888", RoleCode.MANAGER);
        String otherLogin = login("other-session@example.test", PASSWORD);
        String otherAccessToken = accessToken(otherLogin);
        String otherRefreshToken = objectMapper.readTree(otherLogin).at("/data/refreshToken").asText();
        Long otherSessionId = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(other.getId())
                .getFirst()
                .getId();

        mockMvc.perform(get("/api/v1/auth/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(currentSessionId));

        mockMvc.perform(delete("/api/v1/auth/me/sessions/{sessionId}", otherSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/v1/auth/me/sessions/{sessionId}", currentSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNoContent());
        refreshExpectingUnauthorized(refreshToken);

        mockMvc.perform(delete("/api/v1/auth/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherAccessToken)))
                .andExpect(status().isNoContent());
        refreshExpectingUnauthorized(otherRefreshToken);
    }

    private void registerCustomer(String email, String phone) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", email,
                                "password", "Registration123!",
                                "fullName", "Registered Customer",
                                "phone", phone
                        ))))
                .andExpect(status().isCreated());
    }

    private String login() throws Exception {
        return login(EMAIL, PASSWORD);
    }

    private String login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private User createActiveUser(String email, String phone, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), "Other User");
        user.setPhone(phone);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String accessToken(String loginBody) throws Exception {
        return objectMapper.readTree(loginBody).at("/data/accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void refreshExpectingUnauthorized(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", refreshToken)
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String sha256(String value) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
