package com.mentivo.config;

import com.mentivo.repository.ConversationRepository;
import com.mentivo.security.CustomUserDetailsService;
import com.mentivo.security.JwtTokenProvider;
import com.mentivo.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
    private static final String USER_AUTH_SESSION_KEY = "MENTIVO_USER_AUTH";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ConversationRepository conversationRepository;

    public WebSocketConfig(JwtTokenProvider jwtTokenProvider,
                           CustomUserDetailsService customUserDetailsService,
                           ConversationRepository conversationRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        HandshakeInterceptor authInterceptor = new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                          WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    HttpServletRequest httpRequest = servletRequest.getServletRequest();
                    String token = httpRequest.getParameter("token");
                    if (!StringUtils.hasText(token)) {
                        String authHeader = httpRequest.getHeader("Authorization");
                        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                            token = authHeader.substring(7);
                        }
                    }

                    if (StringUtils.hasText(token)) {
                        try {
                            if (jwtTokenProvider.validateToken(token)) {
                                Long userId = jwtTokenProvider.getUserIdFromJWT(token);
                                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                attributes.put(USER_AUTH_SESSION_KEY, auth);
                                log.debug("Handshake authenticated user {} (id: {}) via query/header token",
                                        userDetails.getUsername(), userId);
                            }
                        } catch (Exception ex) {
                            log.warn("WebSocket handshake token validation failed: {}", ex.getMessage());
                        }
                    }
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
            }
        };

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authInterceptor)
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authInterceptor);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }

                boolean wasMutable = accessor.isMutable();
                if (!wasMutable) {
                    accessor = StompHeaderAccessor.wrap(message);
                }

                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

                // 1. Authenticate STOMP CONNECT command
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    String token = null;
                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                    }

                    UsernamePasswordAuthenticationToken auth = null;
                    if (StringUtils.hasText(token)) {
                        try {
                            if (jwtTokenProvider.validateToken(token)) {
                                Long userId = jwtTokenProvider.getUserIdFromJWT(token);
                                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                                auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            } else {
                                log.warn("Rejected WebSocket CONNECT: Invalid JWT token");
                                throw new AccessDeniedException("Invalid JWT token for WebSocket connection");
                            }
                        } catch (Exception ex) {
                            log.warn("Failed to authenticate WebSocket connection: {}", ex.getMessage());
                            throw new AccessDeniedException("Authentication failed: " + ex.getMessage());
                        }
                    } else if (sessionAttributes != null && sessionAttributes.get(USER_AUTH_SESSION_KEY) instanceof UsernamePasswordAuthenticationToken existingAuth) {
                        auth = existingAuth;
                    }

                    if (auth != null) {
                        accessor.setUser(auth);
                        if (sessionAttributes != null) {
                            sessionAttributes.put(USER_AUTH_SESSION_KEY, auth);
                        }
                        log.debug("Authenticated WebSocket connection for user: {}", auth.getName());
                    } else {
                        log.warn("Rejected unauthenticated WebSocket CONNECT command: Missing or invalid token");
                        throw new AccessDeniedException("Authentication token required for WebSocket connection");
                    }
                }

                // 2. Ensure Principal is restored on subsequent STOMP commands (SUBSCRIBE, SEND, etc.)
                if (accessor.getUser() == null && sessionAttributes != null) {
                    Object storedAuth = sessionAttributes.get(USER_AUTH_SESSION_KEY);
                    if (storedAuth instanceof Authentication auth) {
                        accessor.setUser(auth);
                    }
                }

                // 3. Authorize STOMP SUBSCRIBE commands
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    Principal principal = accessor.getUser();
                    if (destination != null) {
                        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
                            Long authenticatedUserId = userPrincipal.getId();

                            // Validate private user topics: /topic/user/{targetUserId}/**
                            if (destination.startsWith("/topic/user/")) {
                                String[] parts = destination.split("/");
                                if (parts.length >= 4) {
                                    try {
                                        Long targetUserId = Long.parseLong(parts[3]);
                                        if (!targetUserId.equals(authenticatedUserId)) {
                                            log.warn("Security violation: User {} attempted to subscribe to unauthorized topic: {}",
                                                    authenticatedUserId, destination);
                                            throw new AccessDeniedException("Access denied: You cannot subscribe to another user's private channel");
                                        }
                                    } catch (NumberFormatException nfe) {
                                        log.warn("Invalid topic format in subscription: {}", destination);
                                        throw new AccessDeniedException("Invalid user channel destination");
                                    }
                                } else {
                                    throw new AccessDeniedException("Invalid user channel destination");
                                }
                            }

                            // Validate conversation topics: /topic/conversations/{convId}/**
                            if (destination.startsWith("/topic/conversations/")) {
                                String[] parts = destination.split("/");
                                if (parts.length >= 4) {
                                    try {
                                        Long convId = Long.parseLong(parts[3]);
                                        boolean isParticipant = conversationRepository.findById(convId)
                                                .map(c -> c.getUserOne().getId().equals(authenticatedUserId) ||
                                                          c.getUserTwo().getId().equals(authenticatedUserId))
                                                .orElse(false);
                                        if (!isParticipant) {
                                            log.warn("Security violation: User {} attempted to subscribe to unauthorized conversation topic: {}",
                                                    authenticatedUserId, destination);
                                            throw new AccessDeniedException("Access denied: You are not a participant in this conversation");
                                        }
                                    } catch (NumberFormatException nfe) {
                                        log.warn("Invalid conversation destination format: {}", destination);
                                        throw new AccessDeniedException("Invalid conversation destination");
                                    }
                                } else {
                                    throw new AccessDeniedException("Invalid conversation destination");
                                }
                            }
                        } else if (destination.startsWith("/topic/user/") || destination.startsWith("/topic/conversations/")) {
                            log.warn("Rejected unauthenticated subscription to protected topic: {}", destination);
                            throw new AccessDeniedException("Authentication required to subscribe to protected topic");
                        }
                    }
                }

                // 4. Authorize STOMP SEND commands
                if (StompCommand.SEND.equals(accessor.getCommand())) {
                    Principal principal = accessor.getUser();
                    if (principal == null) {
                        log.warn("Rejected unauthenticated STOMP SEND command");
                        throw new AccessDeniedException("Authentication required to send messages");
                    }
                }

                if (!wasMutable) {
                    return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
                }
                return message;
            }
        });
    }
}
