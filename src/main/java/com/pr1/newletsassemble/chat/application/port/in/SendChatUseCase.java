package com.pr1.newletsassemble.chat.application.port.in;

public interface SendChatUseCase {
    void send(Long roomId, Long userId, String content);
}
