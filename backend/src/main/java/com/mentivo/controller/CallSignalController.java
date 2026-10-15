package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.call.CallSignalDto;
import com.mentivo.dto.chat.TypingIndicatorDto;
import com.mentivo.entity.Booking;
import com.mentivo.entity.enums.BookingStatus;
import com.mentivo.repository.BlockRepository;
import com.mentivo.repository.BookingRepository;
import com.mentivo.repository.ConversationRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.security.UserPrincipal;
import com.mentivo.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;

@Controller
@Transactional
@Tag(name = "Call Signaling & WebRTC", description = "WebRTC peer connection signaling and ICE configuration")
public class CallSignalController {

    private static final Logger log = LoggerFactory.getLogger(CallSignalController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final FeatureFlagService featureFlagService;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final ConversationRepository conversationRepository;
    private final BookingRepository bookingRepository;

    @Value("${app.webrtc.turn.urls:}")
    private String turnUrls;

    @Value("${app.webrtc.turn.username:}")
    private String turnUsername;

    @Value("${app.webrtc.turn.credential:}")
    private String turnCredential;

    public CallSignalController(SimpMessagingTemplate messagingTemplate,
                                FeatureFlagService featureFlagService,
                                UserRepository userRepository,
                                BlockRepository blockRepository,
                                ConversationRepository conversationRepository,
                                BookingRepository bookingRepository) {
        this.messagingTemplate = messagingTemplate;
        this.featureFlagService = featureFlagService;
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.conversationRepository = conversationRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/api/call/ice-servers")
    @ResponseBody
    @Operation(summary = "Get WebRTC ICE servers including STUN and configured TURN relay servers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIceServers() {
        List<Map<String, Object>> iceServers = new ArrayList<>();

        // Google Public STUN servers
        iceServers.add(Map.of("urls", List.of(
                "stun:stun.l.google.com:19302",
                "stun:stun1.l.google.com:19302",
                "stun:stun2.l.google.com:19302"
        )));

        // Configurable TURN relay server for restrictive NATs
        if (StringUtils.hasText(turnUrls)) {
            Map<String, Object> turnConfig = new HashMap<>();
            turnConfig.put("urls", Arrays.asList(turnUrls.split(",")));
            if (StringUtils.hasText(turnUsername)) {
                turnConfig.put("username", turnUsername);
            }
            if (StringUtils.hasText(turnCredential)) {
                turnConfig.put("credential", turnCredential);
            }
            iceServers.add(turnConfig);
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "iceServers", iceServers
        )));
    }

    @MessageMapping("/call.signal")
    public void handleCallSignal(@Payload CallSignalDto signal, Principal principal) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            log.warn("Unauthorized call signal rejected: unauthenticated WebSocket connection.");
            return;
        }

        Long senderId = userPrincipal.getId();
        // Overwrite sender identity to prevent client spoofing
        signal.setSenderId(senderId);
        signal.setSenderName(userPrincipal.getFullName());
        String senderImage = userRepository.findById(senderId)
                .map(com.mentivo.entity.User::getProfileImage)
                .orElse(null);
        signal.setSenderImage(senderImage);

        String signalType = signal.getType();
        boolean isTerminationSignal = "CALL_END".equals(signalType) || "CALL_REJECT".equals(signalType) || "CALL_BUSY".equals(signalType);

        if (!featureFlagService.isVideoCallingActive() && !isTerminationSignal) {
            log.warn("Call signal {} rejected: Video calling is currently disabled.", signalType);
            CallSignalDto disabledSignal = new CallSignalDto("CALL_REJECT", signal.getCallId(),
                    0L, "System", null, senderId);
            disabledSignal.setReason("1-to-1 Video calling is currently disabled by administrator.");
            messagingTemplate.convertAndSend("/topic/user/" + senderId + "/call", disabledSignal);
            return;
        }

        Long targetUserId = signal.getTargetUserId();
        if (targetUserId == null || targetUserId.equals(senderId)) {
            log.warn("Invalid call signal: targetUserId is missing or equals sender ID.");
            return;
        }

        // Validate target user exists
        if (!userRepository.existsById(targetUserId)) {
            log.warn("Call signal rejected: target user {} does not exist.", targetUserId);
            return;
        }

        // Validate not blocked
        if (blockRepository.isBlockedBetween(senderId, targetUserId)) {
            log.warn("Call signal rejected: communications blocked between user {} and user {}.", senderId, targetUserId);
            sendRejection(senderId, signal.getCallId(), "Unable to connect call with this user.");
            return;
        }

        // Enforce strict session authorization if call is associated with a booking
        if (signal.getBookingId() != null) {
            Optional<Booking> bookingOpt = bookingRepository.findByIdWithDetails(signal.getBookingId());
            if (bookingOpt.isEmpty()) {
                log.warn("Session call rejected: booking ID {} not found.", signal.getBookingId());
                sendRejection(senderId, signal.getCallId(), "Session booking not found.");
                return;
            }

            Booking booking = bookingOpt.get();
            Long learnerUserId = booking.getLearner().getId();
            Long mentorUserId = booking.getMentor() != null && booking.getMentor().getUser() != null
                    ? booking.getMentor().getUser().getId()
                    : null;

            if (mentorUserId == null) {
                log.warn("Session call rejected: mentor user ID could not be resolved for booking ID {}", signal.getBookingId());
                sendRejection(senderId, signal.getCallId(), "Mentor account is currently unavailable.");
                return;
            }

            boolean isSenderLearner = senderId.equals(learnerUserId);
            boolean isSenderMentor = senderId.equals(mentorUserId);
            boolean isTargetLearner = targetUserId.equals(learnerUserId);
            boolean isTargetMentor = targetUserId.equals(mentorUserId);

            if (!((isSenderLearner && isTargetMentor) || (isSenderMentor && isTargetLearner))) {
                log.warn("Unauthorized session call: sender {} and target {} are not authorized participants for booking {}",
                        senderId, targetUserId, signal.getBookingId());
                sendRejection(senderId, signal.getCallId(), "Unauthorized: You are not an authorized participant in this booking.");
                return;
            }

            if (booking.getStatus() == BookingStatus.CANCELLED) {
                log.warn("Session call rejected: booking {} was cancelled.", signal.getBookingId());
                sendRejection(senderId, signal.getCallId(), "This session booking was cancelled.");
                return;
            }

            if (booking.getStatus() == BookingStatus.COMPLETED) {
                log.warn("Session call rejected: booking {} is already completed.", signal.getBookingId());
                sendRejection(senderId, signal.getCallId(), "This session booking has already concluded.");
                return;
            }

            // Transition CONFIRMED booking to IN_PROGRESS when call answer is received
            if ("CALL_ANSWER".equals(signalType) && booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.IN_PROGRESS);
                bookingRepository.save(booking);
                log.info("Booking {} transitioned to IN_PROGRESS upon call answer", booking.getId());
            }

            // Populate verified session metadata
            if (booking.getMentorSession() != null) {
                signal.setSessionId(booking.getMentorSession().getId());
                signal.setSessionTitle(booking.getMentorSession().getTitle());
            } else if (booking.getTopic() != null) {
                signal.setSessionTitle(booking.getTopic().getName());
            } else if (StringUtils.hasText(booking.getMeetingNotes())) {
                signal.setSessionTitle(booking.getMeetingNotes());
            } else {
                signal.setSessionTitle("Mentorship Session");
            }
        }

        log.debug("Routing WebRTC call signal {} from user {} to target user {}",
                signal.getType(), senderId, targetUserId);

        // Forward signaling packet to the target user's dedicated authenticated call topic
        messagingTemplate.convertAndSend("/topic/user/" + targetUserId + "/call", signal);
    }

    private void sendRejection(Long recipientId, String callId, String reason) {
        CallSignalDto rejectedSignal = new CallSignalDto("CALL_REJECT", callId, 0L, "System", null, recipientId);
        rejectedSignal.setReason(reason);
        messagingTemplate.convertAndSend("/topic/user/" + recipientId + "/call", rejectedSignal);
    }

    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload TypingIndicatorDto typing, Principal principal) {
        if (!featureFlagService.isMessagingActive()) {
            return;
        }

        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            return;
        }

        typing.setUserId(userPrincipal.getId());
        typing.setUserName(userPrincipal.getFullName());

        if (typing.getConversationId() != null) {
            Long convId = typing.getConversationId();
            Long userId = userPrincipal.getId();
            boolean isParticipant = conversationRepository.findById(convId)
                    .map(c -> c.getUserOne().getId().equals(userId) || c.getUserTwo().getId().equals(userId))
                    .orElse(false);

            if (isParticipant) {
                messagingTemplate.convertAndSend("/topic/conversations/" + convId + "/typing", typing);
            } else {
                log.warn("Unauthorized typing indicator from user {} for conversation {}", userId, convId);
            }
        }
    }
}
