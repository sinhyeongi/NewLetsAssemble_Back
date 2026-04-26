package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisKeys;
import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisTtl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;

@Repository
@RequiredArgsConstructor
public class ChatLockRepository {

    private final StringRedisTemplate redis;

    public boolean tryAcquireLeaderLock(long userId){
        String key = ChatRedisKeys.unreadLeaderLock(userId);
        return Boolean.TRUE.equals(
                redis.execute((RedisCallback<Boolean>) con ->
                        con.commands().set(
                                key.getBytes(StandardCharsets.UTF_8),
                                "1".getBytes(StandardCharsets.UTF_8),
                                Expiration.milliseconds(ChatRedisTtl.LEADER_LOCK_TTL.toMillis()),
                                RedisStringCommands.SetOption.SET_IF_ABSENT
                        )
                )
        );
    }

    public boolean tryAcquireFlushLock(long userId){
        String key = ChatRedisKeys.flushLock(userId);
        return Boolean.TRUE.equals(
                redis.execute((RedisCallback<Boolean>) con ->
                        con.commands().set(
                                key.getBytes(StandardCharsets.UTF_8),
                                "1".getBytes(StandardCharsets.UTF_8),
                                Expiration.milliseconds(ChatRedisTtl.FLUSH_LOCK_TTL.toMillis()),
                                RedisStringCommands.SetOption.SET_IF_ABSENT
                        )
                )
        );
    }
}
