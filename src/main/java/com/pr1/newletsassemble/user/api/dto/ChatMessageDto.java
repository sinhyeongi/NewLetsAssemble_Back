package com.pr1.newletsassemble.user.api.dto;

import com.pr1.newletsassemble.chat.domain.Chat;
import com.pr1.newletsassemble.chat.domain.ChatType;

import java.time.Instant;

public record ChatMessageDto(
        long id,
        long partyId,
        long senderId,
        ChatType type,
        String clientMessageId,
        String content,
        boolean deleted,
        Instant createdAt
) {
    public static ChatMessageDto of(Chat c){
        return new ChatMessageDto(
                c.getId(),
                c.getParty().getId(),
                c.getSender().getId(),
                c.getType(),
                c.getClientMessageId(),
                c.isDeleted() ? "" : c.getContent(),
                c.isDeleted(),
                c.getCreatedAt()
        );
    }
}
