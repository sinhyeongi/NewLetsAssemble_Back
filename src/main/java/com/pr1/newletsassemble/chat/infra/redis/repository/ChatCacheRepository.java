package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.domain.Chat;
import com.pr1.newletsassemble.chat.infra.redis.ChatRedisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class ChatCacheRepository {
    private final StringRedisTemplate redis;
    private final ChatRedisProperties properties;

    public void cache(Chat chat){
        long partyId = chat.getParty().getId();
        long chatId = chat.getId();

    }
}
