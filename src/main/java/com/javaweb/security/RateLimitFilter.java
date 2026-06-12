package com.javaweb.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.common.exception.ErrorCode;
import com.javaweb.common.response.ApiErrorResponse;
import com.javaweb.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final String AI_PREFIX = "/api/v1/ai/";
    private static final Map<String, HttpMethod> AUTH_ENDPOINTS = Map.of(
            "/api/v1/auth/register", HttpMethod.POST,
            "/api/v1/auth/login", HttpMethod.POST,
            "/api/v1/auth/refresh-token", HttpMethod.POST,
            "/api/v1/auth/logout", HttpMethod.POST
    );

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Limit limit = resolveLimit(request);
        if (!properties.enabled() || limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Instant now = clock.instant();
        String key = limit.bucket() + ":" + clientKey(request);
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || !current.expiresAt().isAfter(now)) {
                return new Window(now.plus(properties.window()), new AtomicInteger(1));
            }
            current.count().incrementAndGet();
            return current;
        });

        if (window.count().get() > limit.maxRequests()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(properties.window().toSeconds()));
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiErrorResponse.of(
                            ErrorCode.RATE_LIMIT_EXCEEDED,
                            "Too many requests. Please retry later.",
                            request.getRequestURI()
                    )
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Limit resolveLimit(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        if (path.startsWith(AI_PREFIX)) {
            return new Limit("ai", properties.aiRequests());
        }
        HttpMethod requiredMethod = AUTH_ENDPOINTS.get(path);
        if (requiredMethod != null && requiredMethod.equals(method)) {
            return new Limit("auth:" + path, properties.authRequests());
        }
        return null;
    }

    private String clientKey(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && !authorization.isBlank()) {
            return "auth:" + sha256(authorization);
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record Limit(String bucket, int maxRequests) {
    }

    private record Window(Instant expiresAt, AtomicInteger count) {
    }
}
