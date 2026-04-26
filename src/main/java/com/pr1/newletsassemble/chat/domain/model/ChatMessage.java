package com.pr1.newletsassemble.chat.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

@Getter
@Builder
public class ChatMessage {
    private final Long roomId;
    private final Long senderId;
    private final String content;
    private final Duration sentAt;
}
