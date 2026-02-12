package com.pr1.newletsassemble.chat.infra.redis.lua;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Configuration
public class ChatRecentCacheLuaConfig {

    @Bean("recentPushTrimScript")
    public DefaultRedisScript<Long> recentPushTrimScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local key = KEYS[1]
                local val = ARGV[1]
                local maxSize = tonumber(ARGV[2])
                local ttlSec = tonumber(ARGV[3])
                local thresholdSec = tonumber(ARGV[4])
                if not key or not val or not maxSize or maxSize <= 0 or not ttlSec or ttlSec <= 0 then return 0 end
                
                redis.call('LPUSH',key,val)
                redis.call('LTRIM',key,0,maxSize -1)
                if not thresholdSec or thresholdSec <= 0 then
                    thresholdSec = math.floor(ttlSec / 3)
                    if thresholdSec < 1 then thresholdSec = 1
                    end
                end
                local t = redis.call('TTL',key)
                if t == -1 or t == -2 or t <= thresholdSec then
                    redis.call('EXPIRE',key,ttlSec)
                end
                return 1
                """);
        return script;
    }

    /**
     * Optional : list trim only (가끔 max 바뀔 때 유지보수용)
     */
    @Bean("recentTrimOnlyScript")
    public DefaultRedisScript<Long> recentTrimOnlyScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local key = KEYS[1]
                local maxSize = tonumber(ARGV[1])
                if not key or not maxSize or maxSize <= 0 then return 0 end
                redis.call('LTRIM',key,0,maxSize-1)
                return 1
                """);
        return script;
    }
}
