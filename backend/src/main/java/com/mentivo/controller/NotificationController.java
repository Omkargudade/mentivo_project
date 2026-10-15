package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.notification.NotificationDto;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notifications and unread alert counters")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get notifications for current user")
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<NotificationDto> notifications = notificationService.getUserNotifications(currentUserId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get number of unread notifications for current user")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        long count = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        NotificationDto notification = notificationService.markAsRead(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
