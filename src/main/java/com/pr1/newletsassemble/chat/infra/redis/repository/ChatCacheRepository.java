package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.domain.Chat;
import com.pr1.newletsassemble.chat.infra.redis.ChatRedisProperties;
import com.pr1.newletsassemble.chat.infra.redis.keys.ChatCacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public class ChatCacheRepository {
    private final StringRedisTemplate redis;
    private final ChatRedisProperties properties;
    private final DefaultRedisScript<Long> recentPushTrimScript;

    public ChatCacheRepository(StringRedisTemplate redis,
                               ChatRedisProperties properties,
                               @Qualifier("recentPushTrimScript")DefaultRedisScript<Long> recentPushTrimScript){
        this.redis = redis;
        this.properties = properties;
        this.recentPushTrimScript = recentPushTrimScript;
    }
    public boolean pushRecent(long partyId,String payloadJson){
        String key = ChatCacheKeys.partyRecentMessages(partyId);

        long max = properties.chatCache().partyRecentMax();
        long ttlSec = properties.chatCache().partyRecentTtl().toSeconds();

        Long r = redis.execute(
                recentPushTrimScript,
                List.of(key),
                payloadJson,
                String.valueOf(max),
                String.valueOf(ttlSec)
        );
        return r != null && r == 1;
    }

    public List<String> getRecentRaw(long partyId, int limit){
        if(limit <= 0) return List.of();
        String key = ChatCacheKeys.partyRecentMessages(partyId);
        Long size = redis.opsForList().size(key);
        if(size == null || size == 0 ) return List.of();
        long end = Math.min(limit -1L,size - 1L);
        List<String> v = redis.opsForList().range(key,0L,end);
        return v == null ? List.of() : v;
    }
    public void evictParty(long partyId){
        redis.delete(ChatCacheKeys.partyRecentMessages(partyId));
    }
}
