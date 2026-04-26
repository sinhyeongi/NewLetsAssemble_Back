package com.pr1.newletsassemble.chat.application.port.out;

import com.pr1.newletsassemble.chat.domain.Chat;
import com.pr1.newletsassemble.chat.domain.model.ChatMessage;

public interface ChatPersistPort {
    Chat save(ChatMessage message);
}
