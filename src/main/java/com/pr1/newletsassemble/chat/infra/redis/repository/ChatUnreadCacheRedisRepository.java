package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisKeys;
import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisTtl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Repository
@Slf4j
public class ChatUnreadCacheRedisRepository {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> unreadSwapwithNonceScript;

    public ChatUnreadCacheRedisRepository(
            StringRedisTemplate redis,
            @Qualifier("unreadSwapWithNonceScript") DefaultRedisScript<Long> unreadSwapwithNonceScript
    ){
        this.redis = redis;
        this.unreadSwapwithNonceScript = unreadSwapwithNonceScript;
    }

    public Map<Long,Long> getUnreadFresh(long userId){
        return readUnreadHash(ChatRedisKeys.unreadFresh(userId));
    }

    public Map<Long,Long> getUnreadStale(long userId){
        return readUnreadHash(ChatRedisKeys.unreadStale(userId));
    }
    private Map<Long,Long> readUnreadHash(String key){
        Map<Object, Object> raw = redis.opsForHash().entries(key);
        if(raw == null || raw.isEmpty()) return Map.of();
        Map<Long,Long> out = new HashMap<>(raw.size());
        for(var e : raw.entrySet()){
            try{
                long partyId = Long.parseLong(String.valueOf(e.getKey()));
                long cnt = Long.parseLong(String.valueOf(e.getValue()));
                out.put(partyId,Math.max(0L,cnt));
            }catch(Exception ignore){}
        }
        return out;
    }

    public boolean writeUnreadWriteThenSwap(long userId, Map<Long,Long> unread, Instant now){
        if(unread == null ) unread = Map.of();
        String freshKey = ChatRedisKeys.unreadFresh(userId);
        String staleKey = ChatRedisKeys.unreadStale(userId);
        String nonceKey = now.toEpochMilli() + "-" + ThreadLocalRandom.current().nextInt(1_000_000);
        long nonce = Optional.ofNullable(redis.opsForValue().increment(ChatRedisKeys.unreadNonceSeq(userId))).orElse(1L);
        String tmpFreshKey = ChatRedisKeys.unreadFreshTmp(userId,nonceKey);
        String tmpStaleKey = ChatRedisKeys.unreadStaleTmp(userId,nonceKey);

        long freshSec = ChatRedisTtl.UNREAD_FRESH_TTL.getSeconds();
        long staleSec = ChatRedisTtl.UNREAD_STALE_TTL.getSeconds();
        long tmpTTL = ChatRedisTtl.UNREAD_TMP_TTL.getSeconds();

        Map<String,String> payload = new HashMap<>();
        for(var e : unread.entrySet()){
            payload.put(String.valueOf(e.getKey()),String.valueOf(Math.max(0L,e.getValue())));
        }
        try {
            redis.executePipelined((RedisCallback<Object>) con -> {
                byte[] tf = tmpFreshKey.getBytes(StandardCharsets.UTF_8);
                byte[] ts = tmpStaleKey.getBytes(StandardCharsets.UTF_8);

                con.commands().del(tf);
                con.commands().del(ts);

                if (!payload.isEmpty()) {
                    for (var e : payload.entrySet()) {
                        byte[] f = e.getKey().getBytes(StandardCharsets.UTF_8);
                        byte[] v = e.getValue().getBytes(StandardCharsets.UTF_8);
                        con.commands().hSet(tf, f, v);
                        con.commands().hSet(ts, f, v);
                    }
                }
                con.commands().expire(tf, tmpTTL);
                con.commands().expire(ts, tmpTTL);
                return null;
            });
        }catch(Exception e){
            return false;
        }
        Long swap = redis.execute(
                unreadSwapwithNonceScript,
                List.of(
                        freshKey,staleKey,tmpFreshKey,tmpStaleKey,ChatRedisKeys.unreadNonceApplied(userId)
                ),
                String.valueOf(nonce),
                String.valueOf(freshSec),
                String.valueOf(staleSec)
        );

        return swap != null && swap == 1L;
    }
}
