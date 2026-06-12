package com.javaweb.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.info.title").value("Real Estate Management API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/refresh-token'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{userId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{userId}/status'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{userId}/roles'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/properties'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/properties'].get").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/properties'].get.parameters[?(@.name == 'keyword')]"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/properties'].get.parameters[?(@.name == 'propertyTypeId')]"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/properties'].get.parameters[?(@.name == 'minPrice')]"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/properties'].get.parameters[?(@.name == 'status')]"
                ).exists())
                .andExpect(jsonPath("$.paths['/api/v1/properties/{propertyId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/properties/{propertyId}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/properties/{propertyId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/properties/{propertyId}/status'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/properties/{propertyId}/images'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/properties/{propertyId}/images'].post").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/properties/{propertyId}/images/{imageId}'].delete"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/properties/{propertyId}/cover-image/{imageId}'].patch"
                ).exists())
                .andExpect(jsonPath("$.paths['/api/v1/files/upload'].post").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/files/upload'].post.requestBody.content['multipart/form-data']"
                ).exists())
                .andExpect(jsonPath("$.paths['/api/v1/notifications'].get").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/notifications/unread-count'].get"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/notifications/{notificationId}/read'].patch"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/notifications/read-all'].patch"
                ).exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.components.schemas.ApiResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ValidationError").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponse").exists())
                .andExpect(jsonPath("$.components.responses.ValidationError.content['application/json'].example.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.components.responses.Unauthorized.content['application/json'].example.code")
                        .value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.tags[0]").value("Authentication"))
                .andExpect(jsonPath("$.paths['/api/v1/properties'].get.tags[0]").value("Properties"))
                .andExpect(jsonPath("$.paths['/api/v1/properties'].get.responses.400['$ref']")
                        .value("#/components/responses/BadRequest"))
                .andExpect(jsonPath("$.paths['/api/v1/properties'].get.responses.401['$ref']")
                        .value("#/components/responses/Unauthorized"))
                .andExpect(jsonPath("$.paths['/api/v1/properties'].get.responses.403['$ref']")
                        .value("#/components/responses/Forbidden"))
                .andExpect(jsonPath("$.paths['/api/v1/properties'].get.responses.404['$ref']")
                        .value("#/components/responses/NotFound"))
                .andExpect(jsonPath("$.paths['/api/v1/properties'].get.responses.500['$ref']")
                        .value("#/components/responses/InternalServerError"));
    }

    @Test
    void shouldExposeSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }
}
