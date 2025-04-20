package com.windlogs.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Allow both raw WebSocket and SockJS connections
        // First endpoint for direct WebSocket connections (for Postman)
        registry.addEndpoint("/ws-notification")
               .setAllowedOriginPatterns("*"); // Using patterns instead of specific origins
               
        // Second endpoint with SockJS for browser fallback support
        registry.addEndpoint("/ws-notification")
               .setAllowedOriginPatterns("*") // Using patterns for better compatibility
               .withSockJS();
    }

}
