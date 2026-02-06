package com.pr1.newletsassemble.chat.api;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class StompChatController {

    // /app/ping 로 들어오는 STOMP 메시지를 처리
    @MessageMapping("/ping")
    public void ping(@Payload String body) {
        // 테스트용: 아무것도 안 해도 "handler 존재"만으로 부팅 성공
        // 필요하면 log 찍기
    }
}