package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.call.CallSignalDto;
import com.mentivo.dto.chat.TypingIndicatorDto;
import com.mentivo.entity.Conversation;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.entity.enums.CurrentMode;
import com.mentivo.entity.enums.PrimaryRole;
import com.mentivo.repository.BlockRepository;
import com.mentivo.repository.BookingRepository;
import com.mentivo.repository.ConversationRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.security.UserPrincipal;
import com.mentivo.service.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallSignalControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private FeatureFlagService featureFlagService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private CallSignalController callSignalController;

    private UserPrincipal userPrincipal;
    private Principal authPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal(
                1L,
                "mentor@test.com",
                "hashedpassword",
                "Dr. Mentor",
                PrimaryRole.MENTOR,
                CurrentMode.MENTOR,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_MENTOR"))
        );
        authPrincipal = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }

    @Test
    @DisplayName("Should return ICE servers including default STUN servers")
    void testGetIceServers() {
        ResponseEntity<ApiResponse<Map<String, Object>>> response = callSignalController.getIceServers();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> iceServers = (List<Map<String, Object>>) response.getBody().getData().get("iceServers");
        assertFalse(iceServers.isEmpty());
        assertTrue(iceServers.get(0).containsKey("urls"));
    }

    @Test
    @DisplayName("Should forward valid WebRTC call offer to target user's dedicated call topic")
    void testHandleCallSignalValidOffer() {
        when(featureFlagService.isVideoCallingActive()).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(userRepository.existsById(2L)).thenReturn(true);
        when(blockRepository.isBlockedBetween(1L, 2L)).thenReturn(false);

        CallSignalDto signal = new CallSignalDto();
        signal.setType("CALL_OFFER");
        signal.setCallId("call-12345");
        signal.setTargetUserId(2L);
        signal.setSdp(Map.of("type", "offer", "sdp", "v=0..."));

        callSignalController.handleCallSignal(signal, authPrincipal);

        ArgumentCaptor<CallSignalDto> captor = ArgumentCaptor.forClass(CallSignalDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/user/2/call"), captor.capture());

        CallSignalDto routedSignal = captor.getValue();
        assertEquals("CALL_OFFER", routedSignal.getType());
        assertEquals("call-12345", routedSignal.getCallId());
        assertEquals(1L, routedSignal.getSenderId()); // Enforced by controller from authenticated Principal
        assertEquals("Dr. Mentor", routedSignal.getSenderName());
        assertEquals(2L, routedSignal.getTargetUserId());
    }

    @Test
    @DisplayName("Should reject self-calls where targetUserId matches senderId")
    void testHandleCallSignalSelfCall() {
        when(featureFlagService.isVideoCallingActive()).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));

        CallSignalDto signal = new CallSignalDto();
        signal.setType("CALL_OFFER");
        signal.setTargetUserId(1L); // Same as sender

        callSignalController.handleCallSignal(signal, authPrincipal);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should reject call if target user does not exist in repository")
    void testHandleCallSignalTargetNotFound() {
        when(featureFlagService.isVideoCallingActive()).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(userRepository.existsById(999L)).thenReturn(false);

        CallSignalDto signal = new CallSignalDto();
        signal.setType("CALL_OFFER");
        signal.setTargetUserId(999L);

        callSignalController.handleCallSignal(signal, authPrincipal);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should send CALL_REJECT back to sender if communication is blocked")
    void testHandleCallSignalBlocked() {
        when(featureFlagService.isVideoCallingActive()).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(userRepository.existsById(2L)).thenReturn(true);
        when(blockRepository.isBlockedBetween(1L, 2L)).thenReturn(true);

        CallSignalDto signal = new CallSignalDto();
        signal.setType("CALL_OFFER");
        signal.setCallId("call-abc");
        signal.setTargetUserId(2L);

        callSignalController.handleCallSignal(signal, authPrincipal);

        ArgumentCaptor<CallSignalDto> captor = ArgumentCaptor.forClass(CallSignalDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/user/1/call"), captor.capture());

        CallSignalDto rejectSignal = captor.getValue();
        assertEquals("CALL_REJECT", rejectSignal.getType());
        assertEquals("call-abc", rejectSignal.getCallId());
        assertTrue(rejectSignal.getReason().contains("Unable to connect call"));
    }

    @Test
    @DisplayName("Should allow termination signal (CALL_END) through even when feature flag is disabled")
    void testHandleCallSignalTerminationAllowedWhenFeatureDisabled() {
        when(featureFlagService.isVideoCallingActive()).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(userRepository.existsById(2L)).thenReturn(true);
        when(blockRepository.isBlockedBetween(1L, 2L)).thenReturn(false);

        CallSignalDto signal = new CallSignalDto();
        signal.setType("CALL_END");
        signal.setCallId("call-active-hangup");
        signal.setTargetUserId(2L);

        callSignalController.handleCallSignal(signal, authPrincipal);

        // Verification: CALL_END is forwarded to target to cleanly terminate call
        verify(messagingTemplate).convertAndSend(eq("/topic/user/2/call"), any(CallSignalDto.class));
    }

    @Test
    @DisplayName("Should forward typing indicator when user is a verified conversation participant")
    void testHandleTypingIndicatorParticipant() {
        when(featureFlagService.isMessagingActive()).thenReturn(true);

        User u1 = new User(); u1.setId(1L);
        User u2 = new User(); u2.setId(2L);
        Conversation conv = new Conversation();
        conv.setId(5L);
        conv.setUserOne(u1);
        conv.setUserTwo(u2);

        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        TypingIndicatorDto typing = new TypingIndicatorDto();
        typing.setConversationId(5L);
        typing.setTyping(true);

        callSignalController.handleTypingIndicator(typing, authPrincipal);

        ArgumentCaptor<TypingIndicatorDto> captor = ArgumentCaptor.forClass(TypingIndicatorDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/5/typing"), captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("Dr. Mentor", captor.getValue().getUserName());
    }

    @Test
    @DisplayName("Should reject typing indicator when user is not a conversation participant")
    void testHandleTypingIndicatorNonParticipant() {
        when(featureFlagService.isMessagingActive()).thenReturn(true);

        User u2 = new User(); u2.setId(2L);
        User u3 = new User(); u3.setId(3L);
        Conversation conv = new Conversation();
        conv.setId(5L);
        conv.setUserOne(u2);
        conv.setUserTwo(u3); // User 1 is NOT a participant

        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        TypingIndicatorDto typing = new TypingIndicatorDto();
        typing.setConversationId(5L);
        typing.setTyping(true);

        callSignalController.handleTypingIndicator(typing, authPrincipal);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should route session call signal when sender and target are authorized booking participants")
    void testHandleCallSignalSessionAuthorized() {
        when(featureFlagService.isVideoCallingActive()).thenReturn(true);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(blockRepository.isBlockedBetween(1L, 2L)).thenReturn(false);

        User mentorUser = new User(); mentorUser.setId(1L);
        com.mentivo.entity.MentorProfile mentorProfile = new com.mentivo.entity.MentorProfile();
        mentorProfile.setId(99L);
        mentorProfile.setUser(mentorUser);

        User learnerUser = new User(); learnerUser.setId(2L);

        com.mentivo.entity.Booking booking = new com.mentivo.entity.Booking();
        booking.setId(10L);
        booking.setMentor(mentorProfile);
        booking.setLearner(learnerUser);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setMeetingNotes("Architecture Review");

        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));

        CallSignalDto signal = new CallSignalDto("CALL_OFFER", "call_123", 1L, "Dr. Mentor", null, 2L);
        signal.setBookingId(10L);

        callSignalController.handleCallSignal(signal, authPrincipal);

        ArgumentCaptor<CallSignalDto> captor = ArgumentCaptor.forClass(CallSignalDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/user/2/call"), captor.capture());
        assertEquals("CALL_OFFER", captor.getValue().getType());
        assertEquals(10L, captor.getValue().getBookingId());
        assertEquals("Architecture Review", captor.getValue().getSessionTitle());
    }

    @Test
    @DisplayName("Should reject session call signal when user is not an authorized participant in the booking")
    void testHandleCallSignalSessionUnauthorized() {
        when(featureFlagService.isVideoCallingActive()).thenReturn(true);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(blockRepository.isBlockedBetween(1L, 2L)).thenReturn(false);

        User otherMentorUser = new User(); otherMentorUser.setId(88L);
        com.mentivo.entity.MentorProfile mentorProfile = new com.mentivo.entity.MentorProfile();
        mentorProfile.setUser(otherMentorUser);

        User learnerUser = new User(); learnerUser.setId(2L);

        com.mentivo.entity.Booking booking = new com.mentivo.entity.Booking();
        booking.setId(10L);
        booking.setMentor(mentorProfile);
        booking.setLearner(learnerUser);
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(booking));

        CallSignalDto signal = new CallSignalDto("CALL_OFFER", "call_123", 1L, "Dr. Mentor", null, 2L);
        signal.setBookingId(10L);

        callSignalController.handleCallSignal(signal, authPrincipal);

        ArgumentCaptor<CallSignalDto> captor = ArgumentCaptor.forClass(CallSignalDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/user/1/call"), captor.capture());
        assertEquals("CALL_REJECT", captor.getValue().getType());
        assertTrue(captor.getValue().getReason().contains("Unauthorized"));
    }
}
