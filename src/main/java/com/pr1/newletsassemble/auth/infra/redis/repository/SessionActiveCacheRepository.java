package com.pr1.newletsassemble.auth.infra.redis.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class SessionActiveCacheRepository {
    private final StringRedisTemplate redisTemplate;

    SessionActiveCacheRepository(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> get(String key){
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }
    public void set(String key ,String sid, Duration ttl){
        redisTemplate.opsForValue().set(key,sid,ttl);
    }
    public void delete(String key){
        redisTemplate.delete(key);
    }
}
