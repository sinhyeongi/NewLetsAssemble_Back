package com.pr1.newletsassemble.chat.infra.redis.lua;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class ChatCoreLuaConfig {
    @Bean("presenceTouchScript")
    public DefaultRedisScript<Long> presenceTouchScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local key = KEYS[1]
                local member = ARGV[1]
                local exp = tonumber(ARGV[2])
                if not key or not member or not exp then
                    return 0
                end
                redis.call('ZADD',key,exp,member)
                return 1
                """);
        return script;
    }

    @Bean("presenceCleanupScript")
    public DefaultRedisScript<Long> presenceCleanupScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                if not key or not now then
                    return 0
                end
                return redis.call('ZREMRANGEBYSCORE',key,'-inf',now)
                """);
        return script;
    }

    @Bean("zsetMaxUpdateScript")
    public DefaultRedisScript<Long> zsetMaxUpdateScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local key = KEYS[1]
                local member = ARGV[1]
                local newScore = tonumber(ARGV[2])
                if not key or not member or not newScore then
                    return 0
                end
                
                local oldScore = redis.call('ZSCORE',key,member)
                if not oldScore then
                    redis.call('ZADD',key,newScore,member)
                    return 1
                end
                
                oldScore = tonumber(oldScore)
                if newScore > oldScore then
                    redis.call('ZADD',key,newScore,member)
                    return 1
                end
                return 0
                """);
        return script;
    }
}
