package com.example.messagerie.websocket;

import com.example.messagerie.security.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Declare l'adresse du canal temps reel : ws://<serveur>:8080/ws/messages?token=<jeton>
 * Le jeton est verifie par l'intercepteur d'Ashley avant l'ouverture de la connexion.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                           JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/messages")
                .addInterceptors(jwtHandshakeInterceptor)
                // Memes origines que le CORS de SecurityConfig : PC local et reseau Wi-Fi de la demo
                .setAllowedOriginPatterns(
                        "http://localhost:[*]",
                        "http://127.0.0.1:[*]",
                        "http://192.168.*:[*]",
                        "http://172.*:[*]",
                        "http://10.*:[*]");
    }
}