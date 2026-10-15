package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.chat.ConversationDto;
import com.mentivo.dto.chat.MessageDto;
import com.mentivo.dto.chat.SendMessageRequest;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/conversations", "/api/chat/conversations"})
@Tag(name = "Real-Time Chat", description = "One-to-one chat conversations and messaging")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    @Operation(summary = "Get list of conversations for current user")
    public ResponseEntity<ApiResponse<Page<ConversationDto>>> getMyConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<ConversationDto> conversations = chatService.getUserConversations(currentUserId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread messages count for current user")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        long count = chatService.getTotalUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation by ID")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversationById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConversationDto conv = chatService.getConversationById(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success(conv));
    }

    @PostMapping
    @Operation(summary = "Open or start conversation with another user")
    public ResponseEntity<ApiResponse<ConversationDto>> getOrCreateConversation(
            @RequestBody Map<String, Long> payload) {
        Long peerId = payload.get("peerId");
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConversationDto conversation = chatService.getOrCreateConversation(currentUserId, peerId);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @PostMapping("/with/{peerId}")
    @Operation(summary = "Open or start conversation with another user by path ID")
    public ResponseEntity<ApiResponse<ConversationDto>> getOrCreateConversationByPath(
            @PathVariable Long peerId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConversationDto conversation = chatService.getOrCreateConversation(currentUserId, peerId);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get messages for a conversation")
    public ResponseEntity<ApiResponse<Page<MessageDto>>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<MessageDto> messages = chatService.getConversationMessages(currentUserId, id, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark all unread incoming messages in a conversation as read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        chatService.markConversationAsRead(currentUserId, id);
        long remainingUnread = chatService.getTotalUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Conversation marked as read", Map.of(
                "conversationId", id,
                "totalUnread", remainingUnread
        )));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send a message in a conversation")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        request.setConversationId(id);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MessageDto sent = chatService.sendMessage(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(sent));
    }
}
