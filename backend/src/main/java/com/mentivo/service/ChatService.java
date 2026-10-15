package com.mentivo.service;

import com.mentivo.dto.chat.ConversationDto;
import com.mentivo.dto.chat.MessageDto;
import com.mentivo.dto.chat.SendMessageRequest;
import com.mentivo.entity.Conversation;
import com.mentivo.entity.Message;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.MessageType;
import com.mentivo.entity.enums.NotificationType;
import com.mentivo.entity.enums.ReadStatus;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.repository.BlockRepository;
import com.mentivo.repository.ConversationRepository;
import com.mentivo.repository.MessageRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.util.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final FeatureFlagService featureFlagService;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository,
                       BlockRepository blockRepository,
                       NotificationService notificationService,
                       SimpMessagingTemplate messagingTemplate,
                       FeatureFlagService featureFlagService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
        this.featureFlagService = featureFlagService;
    }

    @Transactional(readOnly = true)
    public Page<ConversationDto> getUserConversations(Long userId, Pageable pageable) {
        Page<Conversation> conversations = conversationRepository.findUserConversations(userId, pageable);
        return conversations.map(conv -> {
            long unread = messageRepository.countByConversationIdAndRecipientIdAndReadStatus(conv.getId(), userId, ReadStatus.UNREAD);
            return DtoMapper.toConversationDto(conv, userId, unread);
        });
    }

    @Transactional(readOnly = true)
    public ConversationDto getConversationById(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

        if (!conversation.getUserOne().getId().equals(userId) && !conversation.getUserTwo().getId().equals(userId)) {
            throw new UnauthorizedException("You are not a participant in this conversation");
        }

        long unread = messageRepository.countByConversationIdAndRecipientIdAndReadStatus(conversation.getId(), userId, ReadStatus.UNREAD);
        return DtoMapper.toConversationDto(conversation, userId, unread);
    }

    @Transactional
    public ConversationDto getOrCreateConversation(Long userId, Long peerId) {
        if (!featureFlagService.isMessagingActive()) {
            throw new BadRequestException("Direct messaging is currently disabled by administrator.");
        }

        if (userId.equals(peerId)) {
            throw new BadRequestException("You cannot start a conversation with yourself");
        }

        if (blockRepository.isBlockedBetween(userId, peerId)) {
            throw new BadRequestException("Unable to start conversation with this user");
        }

        Conversation conversation = conversationRepository.findBetweenUsers(userId, peerId)
                .orElseGet(() -> {
                    User u1 = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    User u2 = userRepository.findById(peerId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + peerId));
                    return conversationRepository.save(new Conversation(u1, u2));
                });

        long unread = messageRepository.countByConversationIdAndRecipientIdAndReadStatus(conversation.getId(), userId, ReadStatus.UNREAD);
        return DtoMapper.toConversationDto(conversation, userId, unread);
    }

    @Transactional
    public Page<MessageDto> getConversationMessages(Long userId, Long conversationId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

        if (!conversation.getUserOne().getId().equals(userId) && !conversation.getUserTwo().getId().equals(userId)) {
            throw new UnauthorizedException("You are not a participant in this conversation");
        }

        // Mark incoming unread messages as read
        messageRepository.markMessagesAsRead(conversationId, userId);

        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
        return messages.map(DtoMapper::toMessageDto);
    }

    @Transactional
    public void markConversationAsRead(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

        if (!conversation.getUserOne().getId().equals(userId) && !conversation.getUserTwo().getId().equals(userId)) {
            throw new UnauthorizedException("You are not a participant in this conversation");
        }

        messageRepository.markMessagesAsRead(conversationId, userId);

        // Broadcast read receipt event to conversation topic
        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/read",
                    Map.of("conversationId", conversationId, "readerId", userId));
        } catch (Exception ignored) {}
    }

    @Transactional(readOnly = true)
    public long getTotalUnreadCount(Long userId) {
        return messageRepository.countByRecipientIdAndReadStatus(userId, ReadStatus.UNREAD);
    }

    @Transactional
    public MessageDto sendMessage(Long senderId, SendMessageRequest request) {
        if (!featureFlagService.isMessagingActive()) {
            throw new BadRequestException("Direct messaging is currently disabled by administrator.");
        }

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + request.getConversationId()));

        if (!conversation.getUserOne().getId().equals(senderId) && !conversation.getUserTwo().getId().equals(senderId)) {
            throw new UnauthorizedException("You are not a participant in this conversation");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found: " + senderId));

        User recipient = conversation.getUserOne().getId().equals(senderId)
                ? conversation.getUserTwo()
                : conversation.getUserOne();

        if (blockRepository.isBlockedBetween(sender.getId(), recipient.getId())) {
            throw new BadRequestException("Unable to send message to this user");
        }

        String content = request.getContent() != null ? request.getContent().trim() : "";
        if (content.isEmpty()) {
            throw new BadRequestException("Message content cannot be empty.");
        }
        if (content.length() > 5000) {
            throw new BadRequestException("Message content exceeds maximum allowed length of 5000 characters.");
        }

        Message message = new Message(
                conversation,
                sender,
                recipient,
                content,
                request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT
        );
        Message saved = messageRepository.save(message);

        // Update conversation summary
        conversation.setLastMessageAt(LocalDateTime.now());
        String preview = request.getContent().trim();
        if (preview.length() > 100) preview = preview.substring(0, 97) + "...";
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        MessageDto messageDto = DtoMapper.toMessageDto(saved);

        // Broadcast to conversation topic and recipient user topic
        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId(), messageDto);
            messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/messages", messageDto);
            messagingTemplate.convertAndSend("/topic/user/" + recipient.getId() + "/messages", messageDto);
            messagingTemplate.convertAndSend("/topic/user/" + recipient.getId() + "/notifications",
                    Map.of("type", "NEW_MESSAGE", "conversationId", conversation.getId(), "senderName", sender.getFullName()));
        } catch (Exception ignored) {}

        // In-app notification for recipient
        notificationService.createNotification(
                recipient,
                "New message from " + sender.getFullName(),
                preview,
                NotificationType.NEW_MESSAGE,
                conversation.getId()
        );

        return messageDto;
    }
}
