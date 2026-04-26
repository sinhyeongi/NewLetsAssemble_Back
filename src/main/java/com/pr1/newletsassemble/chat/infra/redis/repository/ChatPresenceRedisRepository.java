package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisKeys;
import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisTtl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@Slf4j
public class ChatPresenceRedisRepository {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> presenceTouchScript;
    private final DefaultRedisScript<Long> presenceCleanupScript;

    public ChatPresenceRedisRepository(
            StringRedisTemplate redis,
            @Qualifier("presenceTouchScript") DefaultRedisScript<Long> presenceTouchScript,
            @Qualifier("presenceCleanupScript") DefaultRedisScript<Long> presenceCleanupScript
    ){
        this.redis = redis;
        this.presenceTouchScript = presenceTouchScript;
        this.presenceCleanupScript = presenceCleanupScript;
    }
    public boolean touchPresence(long partyId, long userId, Instant now){
        long expAt = now.plus(ChatRedisTtl.PRESENCE_TTL).toEpochMilli();
        Long r = redis.execute(
                presenceTouchScript,
                List.of(ChatRedisKeys.partyPresence(partyId)),
                String.valueOf(userId),
                String.valueOf(expAt)
        );
        return r != null && r == 1L;
    }
    public void cleanupPresence(long partyId,Instant now){
        try{
            redis.execute(presenceCleanupScript,
                    List.of(ChatRedisKeys.partyPresence(partyId)),
                    String.valueOf(now.toEpochMilli())
            );
        }catch(Exception ignored){
            log.warn("presence cleanup failed partyId={} ",partyId,ignored);
        }
    }
}
