package com.mentivo.config;

import com.mentivo.entity.Conversation;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.CurrentMode;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.repository.ConversationRepository;
import com.mentivo.security.CustomUserDetailsService;
import com.mentivo.security.JwtTokenProvider;
import com.mentivo.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketConfig webSocketConfig;
    private ChannelInterceptor interceptor;

    private UserPrincipal userPrincipal;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig(jwtTokenProvider, customUserDetailsService, conversationRepository);

        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        webSocketConfig.configureClientInboundChannel(registration);
        verify(registration).interceptors(captor.capture());
        interceptor = captor.getValue();

        userPrincipal = new UserPrincipal(
                1L,
                "user1@test.com",
                "pwd",
                "User One",
                PrimaryRole.MENTOR,
                CurrentMode.MENTOR,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_MENTOR"))
        );
        auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }

    @Test
    @DisplayName("Should allow user 1 to subscribe to their own user topic /topic/user/1/call")
    void testSubscribeOwnUserTopicAllowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/user/1/call");
        accessor.setUser(auth);
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(msg, messageChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should deny user 1 from subscribing to user 2's topic /topic/user/2/call")
    void testSubscribeOtherUserTopicDenied() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/user/2/call");
        accessor.setUser(auth);
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(msg, messageChannel));
        assertTrue(exception.getMessage().contains("You cannot subscribe to another user's private channel"));
    }

    @Test
    @DisplayName("Should allow participant to subscribe to conversation topic /topic/conversations/10")
    void testSubscribeConversationParticipantAllowed() {
        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);
        Conversation conv = new Conversation();
        conv.setId(10L);
        conv.setUserOne(u1);
        conv.setUserTwo(u2);

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conv));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversations/10");
        accessor.setUser(auth);
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(msg, messageChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should deny non-participant from subscribing to conversation topic /topic/conversations/10")
    void testSubscribeConversationNonParticipantDenied() {
        User u2 = new User(); u2.setId(2L);
        User u3 = new User(); u3.setId(3L);
        Conversation conv = new Conversation();
        conv.setId(10L);
        conv.setUserOne(u2);
        conv.setUserTwo(u3); // User 1 is NOT a participant

        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conv));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversations/10");
        accessor.setUser(auth);
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(msg, messageChannel));
        assertTrue(exception.getMessage().contains("You are not a participant"));
    }

    @Test
    @DisplayName("Should deny unauthenticated STOMP SEND command")
    void testUnauthenticatedSendDenied() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/call.signal");
        // No user set, no session attributes
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(msg, messageChannel));
    }

    @Test
    @DisplayName("Should restore authentication from session attributes if accessor.getUser() is null")
    void testRestoreAuthFromSessionAttributes() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/call.signal");
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("MENTIVO_USER_AUTH", auth);
        accessor.setSessionAttributes(sessionAttributes);

        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(msg, messageChannel);
        assertNotNull(result);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals("user1@test.com", resultAccessor.getUser().getName());
    }
}
