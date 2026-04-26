package com.pr1.newletsassemble.chat.infra.redis;

import com.pr1.newletsassemble.chat.application.port.out.ChatPublisherPort;
import com.pr1.newletsassemble.chat.domain.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ChatRedisPublisher implements ChatPublisherPort {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    @Override
    public void publish(ChatMessage message) {
        try{
            String payload = objectMapper.writeValueAsString(message);
            redis.convertAndSend("chat.room."+message.getRoomId(),payload);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
