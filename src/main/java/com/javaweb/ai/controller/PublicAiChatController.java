package com.javaweb.ai.controller;

import com.javaweb.ai.dto.ChatMessageRequest;
import com.javaweb.ai.dto.ChatSessionCreateRequest;
import com.javaweb.ai.dto.ChatSessionResponse;
import com.javaweb.ai.service.ChatService;
import com.javaweb.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public/ai/chat/sessions")
public class PublicAiChatController {
    public static final String GUEST_SESSION_HEADER = "X-Guest-Session-Id";

    private final ChatService chatService;

    public PublicAiChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            @Valid @RequestBody(required = false) ChatSessionCreateRequest request,
            @RequestHeader(name = GUEST_SESSION_HEADER, required = false) String guestSessionId
    ) {
        ChatSessionCreateRequest safeRequest = request == null
                ? new ChatSessionCreateRequest(null)
                : request;
        ChatSessionResponse response = chatService.createGuestSession(safeRequest, guestSessionId);
        return ResponseEntity.ok()
                .header(GUEST_SESSION_HEADER, response.guestSessionId())
                .body(ApiResponse.success("Guest chat session created successfully", response));
    }

    @PostMapping("/{sessionId}/messages")
    public ApiResponse<ChatSessionResponse> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatMessageRequest request,
            @RequestHeader(name = GUEST_SESSION_HEADER) String guestSessionId
    ) {
        return ApiResponse.success(
                "Guest chat message sent successfully",
                chatService.sendGuestMessage(sessionId, request, guestSessionId)
        );
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<ChatSessionResponse> getSession(
            @PathVariable Long sessionId,
            @RequestHeader(name = GUEST_SESSION_HEADER) String guestSessionId
    ) {
        return ApiResponse.success(chatService.getGuestSession(sessionId, guestSessionId));
    }
}
