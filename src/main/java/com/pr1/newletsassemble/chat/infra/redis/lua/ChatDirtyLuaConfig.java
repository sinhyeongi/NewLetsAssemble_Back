package com.pr1.newletsassemble.chat.infra.redis.lua;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Configuration
public class ChatDirtyLuaConfig {
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
                local key = KEYS[1]
                local proc = KEYS[2]
                local now = tonumber(ARGV[1])
                local lim = tonumber(ARGV[2])
                if not key or not proc or not now or not lim or lim <=0 then return {} end
                local users = redis.call('ZRANGEBYSCORE',proc,'-inf',now,'LIMIT',lim)
                if not users or #users == 0 then return {} end
                for _, u in ipairs(users) do
                    redis.call('ZREM',proc,u)
                    redis.call('ZADD',key,now,u)
                end
                return users
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
    /**
     * ack success
     * KEYS[1] = dirty Processing
     * KEYS[2] = dirtyUserParties
     * KEYS[3] = dirtyRetry
     * ARGV[1] = userId
     * @return 1/0
     */
    @Bean("dirtyAckScript")
    public DefaultRedisScript<Long> dirtyAckScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local key = KEYS[1]
                local parties = KEYS[2]
                local retry = KEYS[3]
                local userId = ARGV[1]
                if not key or not parties or not retry or not userId then return 0 end
                redis.call('ZREM',key,userId)
                redis.call('DEL',parties)
                redis.call('DEL',retry)
                return 1
                """);
        return script;
    }
    @Bean("dirtyNackScript")
    public DefaultRedisScript<Long> dirtyNackScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local key = KEYS[1]
                local proc = KEYS[2]
                local parties = KEYS[3]
                
                local userId = ARGV[1]
                local nextAt = tonumber(ARGV[2])
                local partiesTtlMs = tonumber(ARGV[3])
                
                if not key or not proc or not userId or not nextAt then return 0 end
                redis.call('ZREM',proc,userId)
                redis.call('ZADD',key,nextAt,userId)
                
                local pttl = redis.call('PTTL',parties)
                if pttl < 0 or pttl < partiesTtlMs then
                    redis.call('PEXPIRE',parties,partiesTtlMs)
                end
                
                return 1
                """);
        return script;
    }
}
