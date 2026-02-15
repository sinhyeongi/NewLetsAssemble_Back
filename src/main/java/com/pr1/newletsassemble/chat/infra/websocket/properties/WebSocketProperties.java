package com.pr1.newletsassemble.chat.infra.websocket.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.websocket")
public record WebSocketProperties(
        List<String> allowedOrigins,
        String endpoint
) {
    public String[] allowedOriginsArray(){
        if(allowedOrigins == null|| allowedOrigins.isEmpty()) return new String[0];
        return allowedOrigins.toArray(String[]:: new);
    }
}
