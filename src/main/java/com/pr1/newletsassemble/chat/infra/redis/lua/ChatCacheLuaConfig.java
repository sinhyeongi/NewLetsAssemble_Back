package com.pr1.newletsassemble.chat.infra.redis.lua;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Configuration
public class ChatCacheLuaConfig {

    /*
     *  삭제 예정
     *  - dirtyPopDueUsersScript
     *  -
     */

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
    @Bean("dirtyMarkScript")
    public DefaultRedisScript<Long> dirtyMarkScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local zkey = KEYS[1]
                local skey = KEYS[2]
                local userId = ARGV[1]
                local partyId = ARGV[2]
                local nextAt = tonumber(ARGV[3])
                local ttlMs = tonumber(ARGV[4])
                if not zkey or not skey or not userId or not partyId or not nextAt or not ttlMs then
                    return 0
                end
                
                redis.call('ZADD',zkey,nextAt,userId)
                redis.call('SADD',skey,partyId)
                local pttl = redis.call('PTTL',skey)
                local now = redis.call('TIME')
                local nowMs = now[1]*1000 + math.floor(now[2]/1000)
                local need = nextAt - nowMs + 6000
                
                if pttl < 0 or pttl < need then
                    redis.call('PEXPIRE',skey,math.max(ttlMs,need))
                end
                return 1
                """);
        return script;
    }
    @Bean("dirtyPopDueUsersScript")
    public DefaultRedisScript<List> dirtyPopDueUsersScript(){
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local limit = tonumber(ARGV[2])
                if not key or not now or not limit or limit <= 0 then return {} end
                local members = redis.call('ZRANGEBYSCORE',key,'-inf',now, 'LIMIT',0,limit)
                if members and #members then
                    redis.call('DEL',key,unpack(members))
                    return members
                end
                return {}
        """);
        return script;
    }
    @Bean("dirtyClaimDueUsersScript")
    public DefaultRedisScript<List> dirtyClaimDueUsersScript(){
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
                local key = KEYS[1]
                local proc = KEYS[2]
                local now = tonumber(ARGV[1])
                local limit = tonumber(ARGV[2])
                local lease = tonumber(ARGV[3])
                if not key or not proc or not now or not limit or limit <= 0 or not lease then return {} end
                local users = redis.call('ZRANGEBYSCORE',key,'-inf',now,'LIMIT',0,limit)
                if not users or #users == 0 then return {} end
                for _, u in ipairs(users) do
                    redis.call('ZREM',key, u)
                    redis.call('ZADD',proc, now + lease, u)
                end
                return users
                """);
        return script;
    }
    @Bean("dirtyRequeueExpiredProcessingScript")
    public DefaultRedisScript<List> dirtyRequeueExpiredProcessingScript(){
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
                
                """);
        return script;
    }
    @Bean("dirtyFetchPartiesScript")
    public DefaultRedisScript<List> dirtyFetchPartiesScript(){
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
                local skey = KEYS[1]
                if not skey then return {} end
                local parties = redis.call('SMEMBERS',skey)
                return parties
        """);
        return script;
    }
    @Bean("unreadSwapScript")
    public DefaultRedisScript<Long> unreadSwapScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local freshKey = KEYS[1]
                local staleKey = KEYS[2]
                local tmpFresh = KEYS[3]
                local tmpStale = KEYS[4]
                local freshTtl = tonumber(ARGV[1])
                local staleTtl = tonumber(ARGV[2])
                
                if not freshKey or not staleKey or not tmpFresh or not tmpStale or not freshTtl or not staleTtl then
                    return 0
                end
                
                if redis.call('EXISTS', tmpFresh) == 0 or redis.call('EXISTS',tmpStale) == 0 then
                    return 0
                end
                
                redis.call('RENAME',tmpFresh,freshKey)
                redis.call('RENAME',tmpStale,staleKey)
                
                redis.call('EXPIRE',freshKey,reshTtl)
                redis.call('EXPIRE',staleKey,staleTtl)
                return 1
                """);
        return script;
    }
}
