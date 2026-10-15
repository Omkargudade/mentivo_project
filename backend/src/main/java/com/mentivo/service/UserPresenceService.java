package com.mentivo.service;

import com.mentivo.dto.user.UserStatusUpdateDto;
import com.mentivo.entity.MentorProfile;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.OnlineStatus;
import com.mentivo.repository.MentorProfileRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.security.UserPrincipal;
import com.mentivo.util.DtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class UserPresenceService {

    private static final Logger log = LoggerFactory.getLogger(UserPresenceService.class);
    private static final String USER_AUTH_SESSION_KEY = "MENTIVO_USER_AUTH";

    private final UserRepository userRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Active sessions: userId -> Set of STOMP session IDs
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    // Session mapping: sessionId -> userId
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();
    // User manual preference: userId -> preferred status
    private final Map<Long, OnlineStatus> userPreferences = new ConcurrentHashMap<>();
    // Grace period disconnect tasks: userId -> ScheduledFuture
    private final Map<Long, ScheduledFuture<?>> pendingDisconnectTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public UserPresenceService(UserRepository userRepository,
                               MentorProfileRepository mentorProfileRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void setUserPreference(Long userId, OnlineStatus status) {
        userPreferences.put(userId, status);
    }

    public boolean isUserConnected(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Long userId = extractUserId(accessor);

        if (userId != null && sessionId != null) {
            sessionToUser.put(sessionId, userId);
            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

            // Cancel any pending disconnect task (handles page refresh / reconnect)
            ScheduledFuture<?> pendingTask = pendingDisconnectTasks.remove(userId);
            if (pendingTask != null) {
                pendingTask.cancel(false);
                log.debug("Cancelled pending disconnect for user {} due to reconnect on session {}", userId, sessionId);
            }

            log.info("User {} connected on session {}. Active session count: {}",
                    userId, sessionId, userSessions.get(userId).size());

            // If user's preference was ONLINE, ensure presence is ONLINE
            OnlineStatus pref = userPreferences.get(userId);
            if (pref == null) {
                // Initialize preference from DB
                userRepository.findById(userId).ifPresent(u -> {
                    userPreferences.put(userId, u.getOnlineStatus());
                    if (u.getOnlineStatus() == OnlineStatus.ONLINE) {
                        markUserOnline(userId);
                    }
                });
            } else if (pref == OnlineStatus.ONLINE) {
                markUserOnline(userId);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        Long extractedId = sessionToUser.remove(sessionId);
        if (extractedId == null) {
            extractedId = extractUserId(accessor);
        }

        if (extractedId != null) {
            final Long targetUserId = extractedId;
            Set<String> sessions = userSessions.get(targetUserId);
            if (sessions != null) {
                sessions.remove(sessionId);
                log.info("User {} session {} disconnected. Remaining active sessions: {}",
                        targetUserId, sessionId, sessions.size());

                if (sessions.isEmpty()) {
                    userSessions.remove(targetUserId);

                    // 4-second grace period to allow browser reload / page transitions without false offline events
                    ScheduledFuture<?> task = scheduler.schedule(() -> {
                        pendingDisconnectTasks.remove(targetUserId);
                        Set<String> active = userSessions.get(targetUserId);
                        if (active == null || active.isEmpty()) {
                            log.info("User {} has no active sessions after grace period. Setting presence to OFFLINE.", targetUserId);
                            markUserOfflineDueToDisconnect(targetUserId);
                        }
                    }, 4, TimeUnit.SECONDS);

                    ScheduledFuture<?> prev = pendingDisconnectTasks.put(targetUserId, task);
                    if (prev != null) {
                        prev.cancel(false);
                    }
                }
            }
        }
    }

    @Transactional
    public void markUserOnline(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            if (user.getOnlineStatus() != OnlineStatus.ONLINE) {
                user.setOnlineStatus(OnlineStatus.ONLINE);
            }
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);

            Long mentorProfileId = null;
            if (user.getMentorProfile() != null) {
                MentorProfile mp = user.getMentorProfile();
                mentorProfileId = mp.getId();
                mp.setOnlineStatus(OnlineStatus.ONLINE);
                mp.setLastSeen(LocalDateTime.now());
                MentorProfile savedMp = mentorProfileRepository.save(mp);
                messagingTemplate.convertAndSend("/topic/mentors/status", DtoMapper.toMentorCardDto(savedMp));
            }

            broadcastPresence(userId, OnlineStatus.ONLINE, mentorProfileId);
        } catch (Exception e) {
            log.error("Failed to mark user {} online: {}", userId, e.getMessage());
        }
    }

    @Transactional
    public void markUserOfflineDueToDisconnect(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            // Only mark offline if current database status is ONLINE
            if (user.getOnlineStatus() == OnlineStatus.ONLINE) {
                user.setOnlineStatus(OnlineStatus.OFFLINE);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);

                Long mentorProfileId = null;
                if (user.getMentorProfile() != null) {
                    MentorProfile mp = user.getMentorProfile();
                    mentorProfileId = mp.getId();
                    mp.setOnlineStatus(OnlineStatus.OFFLINE);
                    mp.setLastSeen(LocalDateTime.now());
                    MentorProfile savedMp = mentorProfileRepository.save(mp);
                    messagingTemplate.convertAndSend("/topic/mentors/status", DtoMapper.toMentorCardDto(savedMp));
                }

                broadcastPresence(userId, OnlineStatus.OFFLINE, mentorProfileId);
            }
        } catch (Exception e) {
            log.error("Failed to mark user {} offline on disconnect: {}", userId, e.getMessage());
        }
    }

    private void broadcastPresence(Long userId, OnlineStatus status) {
        broadcastPresence(userId, status, null);
    }

    private void broadcastPresence(Long userId, OnlineStatus status, Long mentorProfileId) {
        try {
            UserStatusUpdateDto dto = new UserStatusUpdateDto(userId, status, LocalDateTime.now(), mentorProfileId);
            messagingTemplate.convertAndSend("/topic/users/status", dto);
        } catch (Exception e) {
            log.warn("Failed to broadcast presence update: {}", e.getMessage());
        }
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object authObj = sessionAttributes.get(USER_AUTH_SESSION_KEY);
            if (authObj instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal up) {
                return up.getId();
            }
        }
        return null;
    }
}
