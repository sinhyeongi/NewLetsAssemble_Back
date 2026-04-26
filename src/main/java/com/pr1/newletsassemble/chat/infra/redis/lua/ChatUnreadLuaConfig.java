package com.pr1.newletsassemble.chat.infra.redis.lua;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class ChatUnreadLuaConfig {
    /**
     * unread swap with nonce (원자 1/0) :
     * KEYS[1] : freshKey
     * KEYS[2] : staleKey
     * KEYS[3] : tmpFreshKey
     * KEYS[4] : tmpStaleKey
     * KEYS[5] : nonceKey
     * ARGV[1] : nonce (numeric string)
     * ARGV[2] : freshTtlSec
     * ARGV[3] : staleTtlSec
     * return 1/0
     */
    @Bean("unreadSwapWithNonceScript")
    public DefaultRedisScript<Long> unreadSwapWithNonceScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local freshKey = KEYS[1]
                local staleKey = KEYS[2]
                local tmpFresh = KEYS[3]
                local tmpStale = KEYS[4]
                local nonceKey = KEYS[5]
                
                local nonce = tonumber(ARGV[1])
                local freshTtl = tonumber(ARGV[2])
                local staleTtl = tonumber(ARGV[3])
                
                if not freshKey or not staleKey or not tmpFresh or not tmpStale or not nonceKey
                or not nonce or not freshTtl or not staleTtl then return 0 end
                
                local cur = redis.call('GET',nonceKey)
                if cur then
                    local curNum = tonumber(cur)
                    if curNum and curNum >= nonce then
                        return 0
                    end
                end
                
                if redis.call('EXISTS',tmpFresh) == 0 or redis.call('EXISTS',tmpStale) == 0 then return 0 end
                redis.call('SET',nonceKey,tostring(nonce),'EX',staleTtl)
                
                redis.call('RENAME',tmpFresh,freshKey)
                redis.call('RENAME',tmpStale,staleKey)
                
                redis.call('EXPIRE',freshKey,freshTtl)
                redis.call('EXPIRE',staleKey,staleTtl)
                return 1
                """);
        return script;
    }
}
