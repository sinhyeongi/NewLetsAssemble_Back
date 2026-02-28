package com.pr1.newletsassemble.chat.infra.persistence;

import com.pr1.newletsassemble.chat.application.port.out.ChatPersistPort;
import com.pr1.newletsassemble.chat.domain.Chat;
import com.pr1.newletsassemble.chat.domain.model.ChatMessage;
import com.pr1.newletsassemble.chat.infra.persistence.jpa.ChatJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatPersistAdapter implements ChatPersistPort {
    private final ChatJpaRepository jpa;
    @Override
    public void save(ChatMessage message) {
        jpa.save(Chat.create())
    }
}
