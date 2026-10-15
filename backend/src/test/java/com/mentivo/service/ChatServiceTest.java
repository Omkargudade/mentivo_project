package com.mentivo.service;

import com.mentivo.dto.chat.MessageDto;
import com.mentivo.dto.chat.SendMessageRequest;
import com.mentivo.entity.Conversation;
import com.mentivo.entity.Message;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.MessageType;
import com.mentivo.entity.enums.ReadStatus;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.repository.BlockRepository;
import com.mentivo.repository.ConversationRepository;
import com.mentivo.repository.MessageRepository;
import com.mentivo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private FeatureFlagService featureFlagService;

    @InjectMocks
    private ChatService chatService;

    private User userOne;
    private User userTwo;
    private User stranger;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        userOne = new User();
        userOne.setId(1L);
        userOne.setFullName("Mentor John");
        userOne.setEmail("john@example.com");

        userTwo = new User();
        userTwo.setId(2L);
        userTwo.setFullName("Learner Alice");
        userTwo.setEmail("alice@example.com");

        stranger = new User();
        stranger.setId(99L);
        stranger.setFullName("Eve Intruder");

        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUserOne(userOne);
        conversation.setUserTwo(userTwo);
    }

    @Test
    @DisplayName("Should send message successfully and broadcast to conversation topic and recipient user topic")
    void testSendMessageSuccess() {
        when(featureFlagService.isMessagingActive()).thenReturn(true);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(userOne));
        when(blockRepository.isBlockedBetween(1L, 2L)).thenReturn(false);

        Message savedMessage = new Message();
        savedMessage.setId(101L);
        savedMessage.setConversation(conversation);
        savedMessage.setSender(userOne);
        savedMessage.setRecipient(userTwo);
        savedMessage.setContent("Hello, how can I help you today?");
        savedMessage.setMessageType(MessageType.TEXT);
        savedMessage.setReadStatus(ReadStatus.UNREAD);

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(10L);
        request.setContent("Hello, how can I help you today?");
        request.setMessageType(MessageType.TEXT);

        MessageDto result = chatService.sendMessage(1L, request);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("Hello, how can I help you today?", result.getContent());
        assertEquals(1L, result.getSenderId());
        assertEquals(2L, result.getRecipientId());

        // Verify WebSocket broadcasts were dispatched to both topics
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/10"), any(MessageDto.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/2/messages"), any(MessageDto.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if message content is empty or whitespace")
    void testSendMessageEmptyContent() {
        when(featureFlagService.isMessagingActive()).thenReturn(true);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(userOne));
        when(blockRepository.isBlockedBetween(1L, 2L)).thenReturn(false);

        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(10L);
        request.setContent("   ");

        assertThrows(BadRequestException.class, () -> chatService.sendMessage(1L, request));
        verify(messageRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if message content exceeds 5000 characters")
    void testSendMessageExceedsMaxLength() {
        when(featureFlagService.isMessagingActive()).thenReturn(true);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(userOne));
        when(blockRepository.isBlockedBetween(1L, 2L)).thenReturn(false);

        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(10L);
        request.setContent("A".repeat(5001));

        assertThrows(BadRequestException.class, () -> chatService.sendMessage(1L, request));
        verify(messageRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException if sender is not a participant in the conversation")
    void testSendMessageUnauthorizedParticipant() {
        when(featureFlagService.isMessagingActive()).thenReturn(true);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(10L);
        request.setContent("Unauthorized message");

        assertThrows(UnauthorizedException.class, () -> chatService.sendMessage(99L, request));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException if messaging feature flag is disabled")
    void testSendMessageFeatureDisabled() {
        when(featureFlagService.isMessagingActive()).thenReturn(false);

        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(10L);
        request.setContent("Hello");

        assertThrows(BadRequestException.class, () -> chatService.sendMessage(1L, request));
        verify(conversationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should mark conversation as read and broadcast read receipt")
    void testMarkConversationAsRead() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        chatService.markConversationAsRead(2L, 10L);

        verify(messageRepository).markMessagesAsRead(10L, 2L);
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/10/read"), any(Object.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-participant tries to mark conversation as read")
    void testMarkConversationAsReadUnauthorized() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        assertThrows(UnauthorizedException.class, () -> chatService.markConversationAsRead(99L, 10L));
        verify(messageRepository, never()).markMessagesAsRead(anyLong(), anyLong());
    }
}
