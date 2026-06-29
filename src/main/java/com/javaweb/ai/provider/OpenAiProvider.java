package com.javaweb.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.config.AiProperties;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.ImageAnalysisItemResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.exception.AiProviderException;
import com.javaweb.property.entity.PropertyImage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiProvider implements AiProvider, AiImageAnalysisProvider {
    public static final String PROVIDER_NAME = "openai";

    private final AiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiProvider(
            AiProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(normalizeBaseUrl(properties.baseUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean available() {
        return properties.enabled()
                && properties.hasApiKey()
                && PROVIDER_NAME.equalsIgnoreCase(properties.provider());
    }

    @Override
    public AiCompletionResponse complete(AiCompletionRequest request) {
        OpenAiChatResponse response = chatCompletion(textPayload(request));
        OpenAiChoice choice = firstChoice(response);
        String content = choice == null || choice.message() == null ? "" : nullToEmpty(choice.message().content());
        OpenAiUsage usage = response == null ? null : response.usage();
        return new AiCompletionResponse(
                AiRequestStatus.SUCCESS,
                PROVIDER_NAME,
                response == null || response.model() == null ? properties.model() : response.model(),
                content,
                choice == null ? null : choice.finishReason(),
                usage == null ? null : usage.promptTokens(),
                usage == null ? null : usage.completionTokens(),
                usage == null ? null : usage.totalTokens(),
                null
        );
    }

    @Override
    public List<ImageAnalysisItemResponse> analyze(List<PropertyImage> images) {
        OpenAiChatResponse response = chatCompletion(imagePayload(images));
        OpenAiChoice choice = firstChoice(response);
        String content = choice == null || choice.message() == null ? "" : choice.message().content();
        try {
            ImageAnalysisDocument document = objectMapper.readValue(content, ImageAnalysisDocument.class);
            Map<Long, PropertyImage> imageById = new LinkedHashMap<>();
            for (PropertyImage image : images) {
                imageById.put(image.getId(), image);
            }
            List<ImageAnalysisItemResponse> results = new ArrayList<>();
            if (document.images() == null) {
                return List.of();
            }
            for (ImageAnalysisResult item : document.images()) {
                PropertyImage image = imageById.get(item.imageId());
                if (image != null) {
                    results.add(new ImageAnalysisItemResponse(
                            image.getId(),
                            image.getImageUrl(),
                            item.blurry(),
                            item.dark(),
                            item.duplicateSuspected(),
                            item.irrelevant(),
                            item.suggestedCover(),
                            nullToEmpty(item.caption()),
                            item.issues() == null ? List.of() : item.issues(),
                            nullToEmpty(item.recommendation())
                    ));
                }
            }
            return results;
        } catch (Exception exception) {
            throw new AiProviderException("Unable to parse OpenAI image analysis response", exception);
        }
    }

    private OpenAiChatResponse chatCompletion(Map<String, Object> payload) {
        try {
            payload.put("stream", false);
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return parseChatResponse(responseBody);
        } catch (RestClientResponseException exception) {
            throw new AiProviderException("OpenAI request failed: " + providerError(exception), exception);
        }
    }

    private OpenAiChatResponse parseChatResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new AiProviderException("OpenAI response was empty");
        }
        String trimmed = responseBody.trim();
        if (trimmed.startsWith("data:") || trimmed.contains("\ndata:")) {
            return parseStreamResponse(trimmed);
        }
        try {
            return objectMapper.readValue(trimmed, OpenAiChatResponse.class);
        } catch (Exception exception) {
            throw new AiProviderException("Unable to parse OpenAI response", exception);
        }
    }

    private OpenAiChatResponse parseStreamResponse(String body) {
        StringBuilder content = new StringBuilder();
        String finishReason = null;
        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;
        String model = properties.model();

        for (String line : body.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String data = trimmed.substring("data:".length()).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) {
                continue;
            }
            try {
                OpenAiStreamChunk chunk = objectMapper.readValue(data, OpenAiStreamChunk.class);
                if (chunk.model() != null && !chunk.model().isBlank()) {
                    model = chunk.model();
                }
                if (chunk.usage() != null) {
                    promptTokens = chunk.usage().promptTokens();
                    completionTokens = chunk.usage().completionTokens();
                    totalTokens = chunk.usage().totalTokens();
                }
                OpenAiStreamChoice choice = chunk.choices() == null || chunk.choices().isEmpty()
                        ? null
                        : chunk.choices().getFirst();
                if (choice != null) {
                    if (choice.delta() != null && choice.delta().content() != null) {
                        content.append(choice.delta().content());
                    }
                    if (choice.finishReason() != null && !choice.finishReason().isBlank()) {
                        finishReason = choice.finishReason();
                    }
                }
            } catch (Exception exception) {
                throw new AiProviderException("Unable to parse OpenAI streamed response", exception);
            }
        }

        return new OpenAiChatResponse(
                model,
                List.of(new OpenAiChoice(new OpenAiMessage(content.toString()), finishReason)),
                new OpenAiUsage(promptTokens, completionTokens, totalTokens)
        );
    }

    private Map<String, Object> textPayload(AiCompletionRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.userPrompt()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.model());
        payload.put("messages", messages);
        payload.put("temperature", 0.2);
        return payload;
    }

    private Map<String, Object> imagePayload(List<PropertyImage> images) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of(
                "type", "text",
                "text", """
                        Analyze these real estate images in the context of the linked property.
                        Return only JSON with this shape:
                        {"images":[{"imageId":1,"blurry":false,"dark":false,"duplicateSuspected":false,"irrelevant":false,"suggestedCover":false,"caption":"","issues":[],"recommendation":""}]}
                        Use the provided imageId values exactly.
                        Do not invent property facts, and prefer the property metadata below when judging relevance.
                        """
        ));
        for (PropertyImage image : images) {
            content.add(Map.of(
                    "type", "text",
                    "text", "imageId=" + image.getId() + " propertyContext=" + propertyContext(image)
            ));
            content.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", image.getImageUrl())
            ));
        }

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", content);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.model());
        payload.put("messages", List.of(userMessage));
        payload.put("temperature", 0.1);
        payload.put("response_format", Map.of("type", "json_object"));
        return payload;
    }

    private String propertyContext(PropertyImage image) {
        if (image.getProperty() == null) {
            return "{}";
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("propertyId", image.getProperty().getId());
        payload.put("propertyCode", image.getProperty().getCode());
        payload.put("propertyName", image.getProperty().getName());
        payload.put("propertyType", image.getProperty().getPropertyType() == null
                ? null
                : image.getProperty().getPropertyType().getName());
        payload.put("purpose", image.getProperty().getPurpose() == null
                ? null
                : image.getProperty().getPurpose().name());
        payload.put("status", image.getProperty().getStatus() == null
                ? null
                : image.getProperty().getStatus().name());
        payload.put("bedrooms", image.getProperty().getBedrooms());
        payload.put("bathrooms", image.getProperty().getBathrooms());
        payload.put("landArea", image.getProperty().getLandArea());
        payload.put("floorArea", image.getProperty().getFloorArea());
        payload.put("address", image.getProperty().getAddress() == null
                ? null
                : image.getProperty().getAddress().getFullAddress());
        payload.put("coverImage", image.isCoverImage());
        payload.put("displayOrder", image.getDisplayOrder());
        payload.put("altText", image.getAltText());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build image property context", exception);
        }
    }

    private OpenAiChoice firstChoice(OpenAiChatResponse response) {
        return response == null || response.choices() == null || response.choices().isEmpty()
                ? null
                : response.choices().getFirst();
    }

    private String providerError(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return exception.getStatusCode().toString();
        }
        return body.substring(0, Math.min(body.length(), 1000));
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank()
                ? "https://api.openai.com/v1"
                : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiChatResponse(
            String model,
            List<OpenAiChoice> choices,
            OpenAiUsage usage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiStreamChunk(
            String model,
            List<OpenAiStreamChoice> choices,
            OpenAiUsage usage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiStreamChoice(
            OpenAiDelta delta,
            @JsonProperty("finish_reason")
            String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiDelta(String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiChoice(
            OpenAiMessage message,
            @JsonProperty("finish_reason")
            String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiMessage(String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiUsage(
            @JsonProperty("prompt_tokens")
            Integer promptTokens,
            @JsonProperty("completion_tokens")
            Integer completionTokens,
            @JsonProperty("total_tokens")
            Integer totalTokens
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ImageAnalysisDocument(List<ImageAnalysisResult> images) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ImageAnalysisResult(
            @JsonProperty("imageId")
            Long imageId,
            boolean blurry,
            boolean dark,
            @JsonProperty("duplicateSuspected")
            boolean duplicateSuspected,
            boolean irrelevant,
            @JsonProperty("suggestedCover")
            boolean suggestedCover,
            String caption,
            List<String> issues,
            String recommendation
    ) {
    }
}
