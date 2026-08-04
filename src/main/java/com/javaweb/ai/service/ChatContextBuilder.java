package com.javaweb.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.entity.AiMessage;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.listing.dto.PublicListingResponse;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.mapper.ListingMapper;
import com.javaweb.listing.repository.ListingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class ChatContextBuilder {
    static final int MAX_CANDIDATES = 5;

    private static final String SYSTEM_PROMPT = """
            You are a real-estate assistant for a property management platform.
            Help users clarify buying or renting needs, explain listing information, suggest next steps,
            and recommend contacting an agent when the user needs human support.
            If the user is only greeting or making casual conversation, answer politely as a sales assistant
            and do not recommend listings.
            Start asking buying/renting questions only when the user shows interest in buying, renting,
            viewing, comparing, or finding a property.
            Always respond in the same language as the user's latest message.
            Guardrails:
            - Do not provide legal, tax, investment, loan, or financial advice as a professional conclusion.
            - For complex legal or financial questions, recommend contacting a qualified professional or agent.
            - Do not reveal private owner, customer, internal pricing, identity, phone, email, or account data.
            - Do not claim that appointments, leads, transactions, or contracts were created unless the system explicitly says so.
            - Only use the candidate listings provided in the context.
            - Do not invent a listing, price, bedroom count, district, legal status, or address.
            - Recommend listings only when candidate listings are provided.
            - If the candidate list is empty, ask up to 3 short follow-up questions or say that no exact match was found.
            Keep responses concise and practical.
            """;

    private static final Pattern BUDGET_PATTERN = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(ty|trieu|tr)");
    private static final Pattern AREA_PATTERN = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*m2");
    private static final Pattern BEDROOM_PATTERN = Pattern.compile("(?i)(\\d+)\\s*(pn|phong ngu|bedroom|bed)");

    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final ObjectMapper objectMapper;
    private final ChatbotGuardrails guardrails;

    ChatContextBuilder(
            ListingRepository listingRepository,
            ListingMapper listingMapper,
            ObjectMapper objectMapper,
            ChatbotGuardrails guardrails
    ) {
        this.listingRepository = listingRepository;
        this.listingMapper = listingMapper;
        this.objectMapper = objectMapper;
        this.guardrails = guardrails;
    }

    ChatContext build(String currentMessage, List<AiMessage> history, AuthUserPrincipal actor) {
        return build(currentMessage, history, actor, null);
    }

    ChatContext buildGuest(String currentMessage, List<AiMessage> history, String guestSessionId) {
        return build(currentMessage, history, null, guestSessionId);
    }

    private ChatContext build(
            String currentMessage,
            List<AiMessage> history,
            AuthUserPrincipal actor,
            String guestSessionId
    ) {
        String conversationText = conversationText(history, currentMessage);
        ChatSearchIntent intent = parseIntent(conversationText, currentMessage);
        List<PublicListingResponse> candidates = intent.readyForRecommendations()
                ? retrieveCandidates(intent)
                : List.of();
        return new ChatContext(
                SYSTEM_PROMPT,
                buildUserPrompt(currentMessage, history, intent, candidates),
                metadataJson(actor, guestSessionId, currentMessage, history, intent, candidates),
                buildFallbackReply(intent, candidates),
                candidates
        );
    }

    private List<PublicListingResponse> retrieveCandidates(ChatSearchIntent intent) {
        List<Listing> listings = listingRepository.findRecommendationCandidates(
                intent.purpose(),
                null,
                null,
                null,
                null,
                intent.minBudget(),
                intent.maxBudget(),
                intent.minArea(),
                intent.maxArea(),
                intent.minBedrooms(),
                null,
                PageRequest.of(
                        0,
                        MAX_CANDIDATES,
                        Sort.by(Sort.Direction.DESC, "publishedAt")
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                )
        ).getContent();
        return listings.stream().map(listingMapper::toPublicResponse).toList();
    }

    private String buildUserPrompt(
            String currentMessage,
            List<AiMessage> history,
            ChatSearchIntent intent,
            List<PublicListingResponse> candidates
    ) {
        String responseLanguage = responseLanguage(currentMessage);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("intent", intent.toMap());
        payload.put("responseLanguage", responseLanguage);
        payload.put("candidates", candidates.stream().map(this::candidateMap).toList());
        payload.put("rules", List.of(
                "Respond in " + responseLanguage + ", matching the user's latest message",
                "Do not switch to English unless the user's latest message is English",
                "If the user is only greeting or making casual conversation, answer politely as a sales assistant and do not mention listings",
                "If the user has not shown a buying or renting need, do not ask detailed property requirement questions yet",
                "If the user wants to buy or rent but the request is missing key requirements, ask for budget, preferred area, property type, and bedroom count before recommending listings",
                "Use only the candidate listings from the context",
                "Do not invent any listing, price, bedroom count, or address",
                "Recommend listings only when candidates are present",
                "Ask up to 3 short follow-up questions if the request is still too broad"
        ));

        return """
                Conversation history:
                %s

                Current user message:
                %s

                Context JSON:
                %s
                """.formatted(historyText(history), currentMessage, toJson(payload));
    }

    private String buildFallbackReply(ChatSearchIntent intent, List<PublicListingResponse> candidates) {
        if (guardrails.needsProfessionalReferral(intent.searchText())) {
            return guardrails.fallbackReply(intent.searchText(), null);
        }
        boolean vietnamese = "Vietnamese".equals(intent.responseLanguage());
        if (!intent.realEstateIntent()) {
            if (!vietnamese) {
                return "Hi, I am your real-estate sales assistant. I can help with property information, buying or renting needs, and connecting you with an agent when needed.";
            }
            return "Chao ban, toi la tro ly tu van bat dong san. Toi co the ho tro thong tin du an, nhu cau mua thue va ket noi nhan vien phu trach khi ban can.";
        }
        if (!intent.readyForRecommendations()) {
            if (!vietnamese) {
                return "I can help narrow down suitable properties. Please share whether you want to buy or rent, your budget, preferred area, and desired bedroom count.";
            }
            return "Toi co the ho tro loc bat dong san phu hop. Ban cho toi biet them nhu cau mua hay thue, ngan sach, khu vuc uu tien va so phong mong muon nhe.";
        }
        if (candidates.isEmpty()) {
            if (!vietnamese) {
                return "I could not find a listing that matches those criteria. Please share a more flexible budget, preferred area, or property type so I can search again.";
            }
            return "Chua tim thay listing phu hop trong he thong voi cac tieu chi nay. Ban co the noi ro hon khu vuc, ngan sach linh hoat hoac loai bat dong san de toi loc lai.";
        }
        StringBuilder builder = new StringBuilder();
        if (vietnamese) {
            builder.append("Toi tim duoc ").append(candidates.size()).append(" listing phu hop trong he thong: ");
        } else {
            builder.append("I found ").append(candidates.size()).append(" matching listings in the system: ");
        }
        for (int index = 0; index < candidates.size(); index++) {
            PublicListingResponse candidate = candidates.get(index);
            if (index > 0) {
                builder.append("; ");
            }
            builder.append(candidate.code())
                    .append(" - ")
                    .append(candidate.title())
                    .append(" - ")
                    .append(formatMoney(candidate.askingPrice(), candidate.currency()))
                    .append(" - ")
                    .append(candidate.fullAddress());
        }
        return builder.toString();
    }

    private String conversationText(List<AiMessage> history, String currentMessage) {
        StringBuilder builder = new StringBuilder();
        for (AiMessage message : history) {
            builder.append(message.getRole().name())
                    .append(": ")
                    .append(message.getContent())
                    .append('\n');
        }
        builder.append("USER: ").append(currentMessage);
        return builder.toString();
    }

    private String historyText(List<AiMessage> history) {
        if (history.isEmpty()) {
            return "(no previous messages)";
        }
        return history.stream()
                .limit(20)
                .map(message -> message.getRole().name() + ": " + message.getContent())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("(no previous messages)");
    }

    private ChatSearchIntent parseIntent(String text, String currentMessage) {
        String normalized = normalize(text);
        ListingPurpose purpose = parsePurpose(normalized);
        BigDecimal[] budgetRange = parseBudgetRange(normalized);
        BigDecimal[] areaRange = parseAreaRange(normalized);
        Integer bedrooms = parseBedrooms(normalized);
        boolean realEstateIntent = hasRealEstateIntent(normalized, purpose, budgetRange, areaRange, bedrooms);
        return new ChatSearchIntent(
                purpose,
                budgetRange[0],
                budgetRange[1],
                areaRange[0],
                areaRange[1],
                bedrooms,
                normalized,
                responseLanguage(currentMessage),
                realEstateIntent
        );
    }

    private ListingPurpose parsePurpose(String normalized) {
        if (containsAny(normalized, "thue", "rent", "lease")) {
            return ListingPurpose.RENT;
        }
        if (containsAny(normalized, "mua", "buy", "sale", "purchase")) {
            return ListingPurpose.SALE;
        }
        return null;
    }

    private BigDecimal[] parseBudgetRange(String normalized) {
        Matcher matcher = BUDGET_PATTERN.matcher(normalized);
        List<BigDecimal> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(scaleAmount(matcher.group(1), matcher.group(2)));
            if (values.size() == 2) {
                break;
            }
        }
        if (values.isEmpty()) {
            return new BigDecimal[] {null, null};
        }
        if (values.size() == 1) {
            return new BigDecimal[] {null, values.getFirst()};
        }
        BigDecimal first = values.getFirst();
        BigDecimal second = values.get(1);
        return first.compareTo(second) <= 0
                ? new BigDecimal[] {first, second}
                : new BigDecimal[] {second, first};
    }

    private BigDecimal[] parseAreaRange(String normalized) {
        Matcher matcher = AREA_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return new BigDecimal[] {null, null};
        }
        BigDecimal area = scaleDecimal(matcher.group(1));
        return new BigDecimal[] {null, area};
    }

    private Integer parseBedrooms(String normalized) {
        Matcher matcher = BEDROOM_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean hasRealEstateIntent(
            String normalized,
            ListingPurpose purpose,
            BigDecimal[] budgetRange,
            BigDecimal[] areaRange,
            Integer bedrooms
    ) {
        return purpose != null
                || budgetRange[0] != null
                || budgetRange[1] != null
                || areaRange[0] != null
                || areaRange[1] != null
                || bedrooms != null
                || containsAny(
                        normalized,
                        "bat dong san",
                        "bds",
                        "nha",
                        "can ho",
                        "chung cu",
                        "dat",
                        "mat bang",
                        "van phong",
                        "listing",
                        "tin dang",
                        "xem nha",
                        "dat lich",
                        "tu van",
                        "gia",
                        "ngan sach",
                        "phong ngu",
                        "phong tam",
                        "dien tich"
                );
    }

    private BigDecimal scaleAmount(String rawValue, String unit) {
        BigDecimal value = scaleDecimal(rawValue);
        String normalizedUnit = unit.toLowerCase(Locale.ROOT);
        if ("ty".equals(normalizedUnit)) {
            return value.multiply(BigDecimal.valueOf(1_000_000_000L));
        }
        return value.multiply(BigDecimal.valueOf(1_000_000L));
    }

    private BigDecimal scaleDecimal(String rawValue) {
        return new BigDecimal(rawValue.replace(",", "."));
    }

    private Map<String, Object> candidateMap(PublicListingResponse candidate) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listingId", candidate.id());
        payload.put("code", candidate.code());
        payload.put("title", candidate.title());
        payload.put("slug", candidate.slug());
        payload.put("purpose", candidate.purpose().name());
        payload.put("status", candidate.status().name());
        payload.put("askingPrice", candidate.askingPrice());
        payload.put("currency", candidate.currency());
        payload.put("bedrooms", candidate.bedrooms());
        payload.put("bathrooms", candidate.bathrooms());
        payload.put("landArea", candidate.landArea());
        payload.put("floorArea", candidate.floorArea());
        payload.put("provinceName", candidate.provinceName());
        payload.put("districtName", candidate.districtName());
        payload.put("wardName", candidate.wardName());
        payload.put("fullAddress", candidate.fullAddress());
        return payload;
    }

    private String metadataJson(
            AuthUserPrincipal actor,
            String guestSessionId,
            String currentMessage,
            List<AiMessage> history,
            ChatSearchIntent intent,
            List<PublicListingResponse> candidates
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (actor == null) {
            payload.put("actor", Map.of(
                    "type", "GUEST",
                    "guestSessionId", guestSessionId == null ? "" : guestSessionId
            ));
        } else {
            payload.put("actor", Map.of(
                    "type", "AUTHENTICATED",
                    "id", actor.id(),
                    "roles", actor.roles()
            ));
        }
        payload.put("messageCount", history.size());
        payload.put("intent", intent.toMap());
        payload.put("candidateIds", candidates.stream().map(PublicListingResponse::id).toList());
        payload.put("currentMessage", currentMessage);
        return toJson(payload);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build AI chat context", exception);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        String stripped = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return stripped.replaceAll("\\s+", " ").trim();
    }

    private String responseLanguage(String currentMessage) {
        String raw = currentMessage == null ? "" : currentMessage.toLowerCase(Locale.ROOT);
        String normalized = normalize(currentMessage);
        if (raw.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*")
                || containsAny(
                        normalized,
                        "toi",
                        "minh",
                        "ban",
                        "anh",
                        "chi",
                        "em",
                        "ben ban",
                        "cho thue",
                        "mua",
                        "thue",
                        "nha",
                        "can ho",
                        "bao nhieu",
                        "khoang",
                        "ngan sach",
                        "khu vuc",
                        "phong ngu"
                )) {
            return "Vietnamese";
        }
        return "English";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String formatMoney(BigDecimal amount, String currency) {
        if (amount == null) {
            return "Lien he";
        }
        String unit = currency == null || currency.isBlank() ? "VND" : currency;
        return amount.stripTrailingZeros().toPlainString() + " " + unit;
    }

    record ChatContext(
            String systemPrompt,
            String userPrompt,
            String metadataJson,
            String fallbackReply,
            List<PublicListingResponse> suggestedListings
    ) {
    }

    private record ChatSearchIntent(
            ListingPurpose purpose,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            BigDecimal minArea,
            BigDecimal maxArea,
            Integer minBedrooms,
            String searchText,
            String responseLanguage,
            boolean realEstateIntent
    ) {
        boolean readyForRecommendations() {
            return realEstateIntent
                    && purpose != null
                    && (minBudget != null
                    || maxBudget != null
                    || minArea != null
                    || maxArea != null
                    || minBedrooms != null);
        }

        Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("purpose", purpose == null ? null : purpose.name());
            payload.put("minBudget", minBudget);
            payload.put("maxBudget", maxBudget);
            payload.put("minArea", minArea);
            payload.put("maxArea", maxArea);
            payload.put("bedrooms", minBedrooms);
            payload.put("responseLanguage", responseLanguage);
            payload.put("realEstateIntent", realEstateIntent);
            payload.put("readyForRecommendations", readyForRecommendations());
            payload.put("searchText", searchText);
            return payload;
        }
    }
}
