package com.pr1.newletsassemble.auth.infra.redis.repository;

import com.pr1.newletsassemble.auth.api.dto.LoginTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;


import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class LoginReplayCacheRepository {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public record  LockHandle(String lockKey, String ownerToken){}


    private String cacheKey(String idemKey){
        return "login:replay:" + idemKey;
    }
    private String lockKey(String idemKey){
        return "login:lock:" + idemKey;
    }
    public LoginTokens get(String idemKey){
        String json = redis.opsForValue().get(cacheKey(idemKey));
        if(json == null) return null;
        try{
            return objectMapper.readValue(json, LoginTokens.class);
        }catch(Exception e){
            // 캐시가 깨졌다면 miss처리
            redis.delete(cacheKey(idemKey));
            return null;
        }
    }

    public void put(String idemKey, LoginTokens resp, Duration ttl){
        try{
            String json = objectMapper.writeValueAsString(resp);
            redis.opsForValue().set(cacheKey(idemKey),json,ttl);
        }catch(Exception e){
            throw new IllegalStateException("LOGIN_RESULT_SERIALIZE_FAILED",e);
        }
    }

    /**
     * SET lockKey ownerToken NX PX ttl
     */
    public LockHandle tryLock(String idemKey, String ownerToken, Duration ttl){
        Boolean ok = redis.opsForValue().setIfAbsent(lockKey(idemKey),ownerToken,ttl);
        return (ok != null && ok) ? new LockHandle(lockKey(idemKey),ownerToken) : null;
    }

    /**
     * renew : 내가 owner 일때만 ttl 갱신
     */
    public boolean renew(LockHandle handle,Duration ttl){
        String cur = redis.opsForValue().get(handle.lockKey());
        if(cur == null){return false;}
        if(!cur.equals(handle.ownerToken())){return false;}
        redis.expire(handle.lockKey(),ttl);
        return true;
    }

    /**
     *  unlock : 내가 owner일때만 삭제
     */
    public void unlock(LockHandle handle){

        String cur = redis.opsForValue().get(handle.lockKey());
        if(cur == null) return;
        if(!cur.equals(handle.ownerToken())){return;}
        redis.delete(handle.lockKey());
    }
}
