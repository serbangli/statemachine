package com.statemachine.config;

import com.statemachine.websocket.MachineStateWebSocketHandler;

import org.springframework.lang.NonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    @NonNull
    private MachineStateWebSocketHandler machineStateWebSocketHandler;

    @Override    
    public void registerWebSocketHandlers( @NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(machineStateWebSocketHandler, "/ws/machine-updates")
                .setAllowedOrigins("*");
    }
}

