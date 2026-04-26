package com.pr1.newletsassemble.auth.infra.redis.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
public class TokenVersionRepository {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> getOrInitScript;
    private final DefaultRedisScript<Long> bumpScript;
    private final DefaultRedisScript<Long> deleteScript;

    public TokenVersionRepository(
            StringRedisTemplate redis,
            @Qualifier("tokenVersionGetOrInitScript") DefaultRedisScript<Long> getOrInitScript,
            @Qualifier("tokenVersionBumpScript") DefaultRedisScript<Long> bumpScript,
            @Qualifier("tokenVersionDeleteScript") DefaultRedisScript<Long> deleteScript
    ){
        this.redis = redis;
        this.getOrInitScript = getOrInitScript;
        this.bumpScript = bumpScript;
        this.deleteScript = deleteScript;
    }
    private String key(Long userId){
        return "token:" + userId + ":version";
    }
    /** =================== Read ===================
       - 없으면 1로 "간주"만 한다(키 생성 X)
     */
    public long getOrInit(Long userId, Duration ttl){
        Long v = redis.execute(getOrInitScript,
                List.of(key(userId)),
                String.valueOf(ttl.toMillis())
        );
        if(v == null){
            throw new IllegalStateException("TOKEN_VERSION_GET_FAILED userId="+userId);
        }
        return v;
    }

    /** =================== Write ===================
       - 전기기 로그아웃/보안 이벤트에서만 bump
     */
    public long bump(Long userId,Duration ttl){
        Long v = redis.execute(bumpScript,
                List.of(key(userId)),
                String.valueOf(ttl.toMillis())
        );
        if(v == null){
            throw new IllegalStateException("TOKEN_VERSION_BUMP_FAILED userId="+userId);
        }
        return v;
    }
}
