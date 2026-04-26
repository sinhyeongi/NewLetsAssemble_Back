package com.pr1.newletsassemble.auth.infra.redis.lua;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class TokenVersionLuaConfig {

    @Bean("tokenVersionGetOrInitScript")
    public DefaultRedisScript<Long> tokenVersionGetOrInitScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                """
                        local v = redis.call('GET',KEYS[1])
                        if not v then
                            redis.call('PSETEX',KEYS[1],ARGV[1],'1')
                            return 1
                        end
                        redis.call('PEXPIRE',KEYS[1],ARGV[1])
                        return tonumber(v)
                        """
        );
        return script;
    }

    @Bean("tokenVersionBumpScript")
    public DefaultRedisScript<Long> tokenVersionBumpScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);

        script.setScriptText("""
                local v = redis.call('INCR',KEYS[1])
                redis.call('PEXPIRE',KEYS[1],ARGV[1])
                return v
                """);
        return script;
    }

    @Bean("tokenVersionDeleteScript")
    public DefaultRedisScript<Long> tokenVersionDeleteScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("return redis.call('DEL',KEYS[1])");
        return script;
    }
}
