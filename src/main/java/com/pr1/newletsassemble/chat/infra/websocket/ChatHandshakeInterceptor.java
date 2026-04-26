package com.pr1.newletsassemble.chat.infra.websocket;

import com.pr1.newletsassemble.chat.application.port.out.PartyMemberReaderPort;
import com.pr1.newletsassemble.chat.application.port.out.TokenValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
    private final TokenValidator tokenValidator;
    private final PartyMemberReaderPort partyMemberReaderPort;
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {

    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try{
            HttpServletRequest req = ((ServletServerHttpRequest) request).getServletRequest();
            String token = req.getHeader("Authorization");
            Long userId = tokenValidator.validateAndGetUserId(token);
            Long roomId = Long.parseLong(req.getParameter("roomId"));
            if(!partyMemberReaderPort.exists(roomId,userId)){
                return false;
            }
            attributes.put("userId",userId);
            attributes.put("roomId",roomId);
            return true;
        }catch(Exception e){
            return false;
        }
    }
}
