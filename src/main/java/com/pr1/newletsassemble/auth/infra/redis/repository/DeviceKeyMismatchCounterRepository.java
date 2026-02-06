package com.pr1.newletsassemble.auth.infra.redis.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeviceKeyMismatchCounterRepository {
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            """
                    local v = redis.call('INCR',KEYS[1])
                    if v == 1 then
                        redis.call('PEXPIRE',KEYS[1],ARGV[1])
                    end
                    return v
                    """,Long.class);

    public long incrUser(String key, Duration window){
        return redis.execute(INCR_WITH_TTL,
                List.of(key),
                String.valueOf(window.toMillis()));
    }
    public long incrSid(String key, Duration window){
        return redis.execute(INCR_WITH_TTL,
                List.of(key),
                String.valueOf(window.toMillis()));
    }
    public void resetUser(String key){
        redis.delete(key);
    }
    public void resetSid(String key){
        redis.delete(key);
    }
}
