package com.mentivo.service;

import com.mentivo.dto.notification.NotificationDto;
import com.mentivo.entity.Notification;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.NotificationType;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.repository.NotificationRepository;
import com.mentivo.util.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Notification createNotification(User user, String title, String message, NotificationType type, Long relatedId) {
        Notification notification = new Notification(user, title, message, type, relatedId);
        Notification saved = notificationRepository.save(notification);

        // Push real-time alert via WebSocket to user queue
        try {
            NotificationDto dto = DtoMapper.toNotificationDto(saved);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", dto);
        } catch (Exception ignored) {}

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(DtoMapper::toNotificationDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationDto markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to modify this notification");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return DtoMapper.toNotificationDto(saved);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
