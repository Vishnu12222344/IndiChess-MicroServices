package com.Match_Service.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration for Match Service.
 *
 * Handles real-time game updates, matchmaking, and chat.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for pub/sub
        config.enableSimpleBroker("/topic", "/queue");

        // Application destination prefix for client messages
        config.setApplicationDestinationPrefixes("/app");

        // User destination prefix for personal messages
        config.setUserDestinationPrefix("/user");

        log.info("✅ Message broker configured");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register /ws endpoint for WebSocket connections
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Allow all origins
                .withSockJS();  // Enable SockJS fallback for browsers that don't support WebSocket

        log.info("✅ WebSocket endpoint registered at /ws");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add JWT authentication interceptor
        // This validates JWT token on STOMP CONNECT
        registration.interceptors(authInterceptor);

        log.info("✅ JWT authentication interceptor configured");
    }
}