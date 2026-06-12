package com.javaweb.ai.controller;

import com.javaweb.ai.dto.ChatMessageRequest;
import com.javaweb.ai.dto.ChatSessionCreateRequest;
import com.javaweb.ai.dto.ChatSessionResponse;
import com.javaweb.ai.service.ChatService;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/chat/sessions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT', 'MANAGER', 'ADMIN', 'OWNER')")
public class AiChatController {
    private final ChatService chatService;

    public AiChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ApiResponse<ChatSessionResponse> createSession(
            @Valid @RequestBody(required = false) ChatSessionCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        ChatSessionCreateRequest safeRequest = request == null
                ? new ChatSessionCreateRequest(null)
                : request;
        return ApiResponse.success(
                "Chat session created successfully",
                chatService.createSession(safeRequest, actor)
        );
    }

    @PostMapping("/{sessionId}/messages")
    public ApiResponse<ChatSessionResponse> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Chat message sent successfully",
                chatService.sendMessage(sessionId, request, actor)
        );
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<ChatSessionResponse> getSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(chatService.getSession(sessionId, actor));
    }
}
