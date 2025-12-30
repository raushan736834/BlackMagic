package com.blackmagic.BlackMagic.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed.origins}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages from client to server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint for kitchen display
        registry.addEndpoint("/ws/kitchen")
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS();

        // WebSocket endpoint for customer table updates
        registry.addEndpoint("/ws/table")
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS();
    }
}
