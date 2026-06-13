package com.javaweb.config;

import com.javaweb.common.exception.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String JSON = "application/json";

    @Bean
    public OpenAPI realEstateOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                        .addResponses("BadRequest", errorResponse(
                                "Bad request. The request body, path variable, or query parameter is invalid.",
                                ErrorCode.INVALID_REQUEST,
                                "Request body is missing or malformed",
                                "/api/v1/properties"
                        ))
                        .addResponses("ValidationError", validationErrorResponse())
                        .addResponses("Unauthorized", errorResponse(
                                "Missing, expired, or invalid JWT access token.",
                                ErrorCode.UNAUTHORIZED,
                                "Authentication is required",
                                "/api/v1/auth/me"
                        ))
                        .addResponses("Forbidden", errorResponse(
                                "The authenticated user does not have permission for this operation.",
                                ErrorCode.FORBIDDEN,
                                "Access is denied",
                                "/api/v1/admin/users"
                        ))
                        .addResponses("NotFound", errorResponse(
                                "The requested resource does not exist.",
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Resource not found",
                                "/api/v1/properties/1000"
                        ))
                        .addResponses("Conflict", errorResponse(
                                "The request conflicts with a duplicate resource or a business rule.",
                                ErrorCode.BUSINESS_RULE_VIOLATION,
                                "Business rule violation",
                                "/api/v1/listings/15/publish"
                        ))
                        .addResponses("RateLimited", errorResponse(
                                "Too many requests were sent in the configured rate-limit window.",
                                ErrorCode.RATE_LIMIT_EXCEEDED,
                                "Rate limit exceeded",
                                "/api/v1/ai/listing-description"
                        ))
                        .addResponses("InternalServerError", errorResponse(
                                "Unexpected server error.",
                                ErrorCode.INTERNAL_SERVER_ERROR,
                                "An unexpected error occurred",
                                "/api/v1/properties"
                        )))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .tags(apiTags())
                .info(new Info()
                        .title("Real Estate Management API")
                        .description("""
                                REST API for the Real Estate Management backend.

                                All successful responses use ApiResponse. Errors use ApiErrorResponse with a stable code, message, path, timestamp, and optional validation errors. Authenticate in Swagger UI with a JWT access token from POST /api/v1/auth/login.
                                """)
                        .version("v1")
                        .contact(new Contact().name("Real Estate Management Team"))
                        .license(new License().name("Proprietary")));
    }

    @Bean
    public OpenApiCustomizer realEstateOpenApiCustomizer() {
        return openApi -> {
            openApi.getComponents()
                    .addSchemas("ApiResponse", apiResponseSchema())
                    .addSchemas("ApiErrorResponse", apiErrorResponseSchema())
                    .addSchemas("ValidationError", validationErrorSchema())
                    .addSchemas("PageResponse", pageResponseSchema());

            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations().forEach(operation -> {
                operation.setTags(List.of(tagNameForPath(path)));
                operation.setSummary(summaryFor(operation.getOperationId(), path));
                normalizeParameterExamples(operation.getParameters());

                ApiResponses responses = operation.getResponses();
                putIfMissing(responses, "400", "BadRequest");
                putIfMissing(responses, "401", "Unauthorized");
                putIfMissing(responses, "403", "Forbidden");
                putIfMissing(responses, "404", "NotFound");
                putIfMissing(responses, "409", "Conflict");
                putIfMissing(responses, "429", "RateLimited");
                putIfMissing(responses, "500", "InternalServerError");
            }));
        };
    }

    private static Schema<?> apiResponseSchema() {
        return new ObjectSchema()
                .description("Standard success response wrapper")
                .addProperty("success", new BooleanSchema().example(true))
                .addProperty("code", new StringSchema().example("SUCCESS"))
                .addProperty("message", new StringSchema().example("Request processed successfully"))
                .addProperty("data", new ObjectSchema().description("Endpoint-specific response payload"))
                .addProperty("timestamp", new DateTimeSchema().example("2026-06-12T00:00:00Z"));
    }

    private static Schema<?> apiErrorResponseSchema() {
        return new ObjectSchema()
                .description("Standard error response wrapper")
                .addProperty("success", new BooleanSchema().example(false))
                .addProperty("code", new StringSchema().example(ErrorCode.VALIDATION_ERROR))
                .addProperty("message", new StringSchema().example("Request validation failed"))
                .addProperty("errors", new ArraySchema().items(new Schema<>().$ref("#/components/schemas/ValidationError")))
                .addProperty("path", new StringSchema().example("/api/v1/properties"))
                .addProperty("timestamp", new DateTimeSchema().example("2026-06-12T00:00:00Z"));
    }

    private static Schema<?> validationErrorSchema() {
        return new ObjectSchema()
                .description("Field-level validation error")
                .addProperty("field", new StringSchema().example("email"))
                .addProperty("message", new StringSchema().example("must be a well-formed email address"));
    }

    private static Schema<?> pageResponseSchema() {
        return new ObjectSchema()
                .description("Standard paginated response payload")
                .addProperty("content", new ArraySchema().items(new ObjectSchema()))
                .addProperty("page", new IntegerSchema().example(0))
                .addProperty("size", new IntegerSchema().example(20))
                .addProperty("totalElements", new IntegerSchema().format("int64").example(125))
                .addProperty("totalPages", new IntegerSchema().example(7))
                .addProperty("first", new BooleanSchema().example(true))
                .addProperty("last", new BooleanSchema().example(false));
    }

    private static ApiResponse errorResponse(String description, String code, String message, String path) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        JSON,
                        new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))
                                .example(errorExample(code, message, path))
                ));
    }

    private static ApiResponse validationErrorResponse() {
        return new ApiResponse()
                .description("Validation failed for one or more request fields.")
                .content(new Content().addMediaType(
                        JSON,
                        new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))
                                .example(Map.of(
                                        "success", false,
                                        "code", ErrorCode.VALIDATION_ERROR,
                                        "message", "Request validation failed",
                                        "errors", List.of(Map.of(
                                                "field", "email",
                                                "message", "must be a well-formed email address"
                                        )),
                                        "path", "/api/v1/auth/register",
                                        "timestamp", "2026-06-12T00:00:00Z"
                                ))
                ));
    }

    private static Map<String, Object> errorExample(String code, String message, String path) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("success", false);
        example.put("code", code);
        example.put("message", message);
        example.put("errors", List.of());
        example.put("path", path);
        example.put("timestamp", "2026-06-12T00:00:00Z");
        return example;
    }

    private static void putIfMissing(ApiResponses responses, String status, String componentName) {
        if (!responses.containsKey(status)) {
            responses.addApiResponse(status, new ApiResponse().$ref("#/components/responses/" + componentName));
        }
    }

    private static List<Tag> apiTags() {
        return List.of(
                tag("Authentication", "Registration, login, refresh token, logout, and current user identity."),
                tag("User Management", "Admin-only user search, lookup, status, and role management."),
                tag("Properties", "Property inventory, images, cover image, and lifecycle status."),
                tag("Listings", "Listing workflow, public search, favorites, approval, and publishing."),
                tag("Customers", "Customer profiles, notes, requirements, and timeline."),
                tag("Leads", "Lead intake, assignment, status, notes, activities, and follow-up tasks."),
                tag("Appointments", "Viewing appointments, rescheduling, cancellation, completion, and feedback."),
                tag("Contracts", "Contract drafting, document upload, review, approval, signing, and cancellation."),
                tag("Transactions", "Deposits, payment schedules, payments, invoices, and receipts."),
                tag("Commissions", "Commission rules, agent commissions, and payout status."),
                tag("Notifications", "Authenticated notification inbox and read state."),
                tag("Dashboard", "Role-based dashboard summaries."),
                tag("Reports", "Revenue, lead, transaction, and commission reports."),
                tag("AI", "AI-assisted chat, scoring, summaries, recommendations, descriptions, and image analysis."),
                tag("Files", "Authenticated multipart file uploads."),
                tag("Audit Logs", "Admin audit log search and detail lookup.")
        ).stream().sorted(Comparator.comparing(Tag::getName)).toList();
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }

    private static String tagNameForPath(String path) {
        if (path.startsWith("/api/v1/auth")) {
            return "Authentication";
        }
        if (path.startsWith("/api/v1/admin/users")) {
            return "User Management";
        }
        if (path.startsWith("/api/v1/properties")) {
            return "Properties";
        }
        if (path.startsWith("/api/v1/listings") || path.startsWith("/api/v1/search/listings")) {
            return "Listings";
        }
        if (path.startsWith("/api/v1/customers")) {
            return "Customers";
        }
        if (path.startsWith("/api/v1/leads")) {
            return "Leads";
        }
        if (path.startsWith("/api/v1/appointments")) {
            return "Appointments";
        }
        if (path.startsWith("/api/v1/contracts")) {
            return "Contracts";
        }
        if (path.startsWith("/api/v1/transactions")) {
            return "Transactions";
        }
        if (path.startsWith("/api/v1/commission")) {
            return "Commissions";
        }
        if (path.startsWith("/api/v1/notifications")) {
            return "Notifications";
        }
        if (path.startsWith("/api/v1/dashboard")) {
            return "Dashboard";
        }
        if (path.startsWith("/api/v1/reports")) {
            return "Reports";
        }
        if (path.startsWith("/api/v1/ai")) {
            return "AI";
        }
        if (path.startsWith("/api/v1/files")) {
            return "Files";
        }
        if (path.startsWith("/api/v1/audit-logs")) {
            return "Audit Logs";
        }
        return "General";
    }

    private static String summaryFor(String operationId, String path) {
        if (operationId == null || operationId.isBlank()) {
            return "Call " + path;
        }
        String words = operationId
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .replaceAll("\\d+$", "")
                .trim();
        if (words.isBlank()) {
            return "Call " + path;
        }
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static void normalizeParameterExamples(List<Parameter> parameters) {
        if (parameters == null) {
            return;
        }
        parameters.stream()
                .filter(parameter -> parameter.getExample() == null)
                .forEach(parameter -> parameter.setExample(exampleForParameter(parameter.getName())));
    }

    private static Object exampleForParameter(String name) {
        return switch (name) {
            case "page" -> 0;
            case "size" -> 20;
            case "sort" -> "createdAt,desc";
            case "keyword" -> "downtown";
            case "status" -> "ACTIVE";
            case "propertyId", "listingId", "customerId", "leadId", "appointmentId", "contractId",
                    "transactionId", "commissionId", "ruleId", "auditLogId", "notificationId", "imageId",
                    "paymentId", "sessionId", "userId" -> 1;
            default -> null;
        };
    }
}
