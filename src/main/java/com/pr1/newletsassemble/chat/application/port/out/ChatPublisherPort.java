package com.pr1.newletsassemble.chat.application.port.out;

import com.pr1.newletsassemble.chat.domain.Chat;

public interface ChatPublisherPort {
    void publish(Chat chat);
}
