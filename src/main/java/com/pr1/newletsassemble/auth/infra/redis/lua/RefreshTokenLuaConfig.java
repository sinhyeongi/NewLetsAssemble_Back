package com.pr1.newletsassemble.auth.infra.redis.lua;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RefreshTokenLuaConfig {
    /* =================== Script: Save (PSETEX + SADD) ==================
        - KEYS[1] : refresh:{userId}:{sid}
        - KEYS[2] : refresh:idx:{userId}
        - ARGV[1] : hash
        - ARGV[2] : ttlMillis
        - ARGV[3] : sid
     */
    @Bean("refreshSaveScript")
    public DefaultRedisScript<Long> saveRefreshScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                """
                        redis.call('PSETEX',KEYS[1], ARGV[2], ARGV[1])
                        redis.call('SADD', KEYS[2],ARGV[3])
                        return 1
                        """
        );
        return script;
    }
    /* =================== Script: Delete One (DEL + SREM) ==================
        - KEYS[1] : refresh:{userId}:{sid}
        - KEYS[2] : refresh:idx:{userId}
        - ARGV[1] : sid
        - return 결과 ( 0 / 1 )
     */
    @Bean("refreshDeleteOneScript")
    public DefaultRedisScript<Long> deleteOneRefreshScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                """
                        local deleted = redis.call('DEL',KEYS[1])
                        redis.call('SREM',KEYS[2], ARGV[1])
                        return deleted
                        """
        );
        return script;
    }
    /* =================== Script: Delete All (SMEMBERS + DEL + DEL) ==================
        - KEYS[1] : refresh:idx:{userId}
        - ARGV[1] : userId (토큰 키 문자 구성용 )
        - return 삭제된 refresh 키 갯수
     */
    @Bean("refreshDeleteAllScript")
    public DefaultRedisScript<Long> deleteAllRefreshScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                """
                        local idxKey = KEYS[1]
                        local userId = ARGV[1]
                        local sids = redis.call('SMEMBERS',idxKey)
                        local cnt = 0
                        for _, sid in ipairs(sids) do
                        local tokenKey = 'refresh:'.. userId .. ':' .. sid
                        cnt = cnt + redis.call('DEL',tokenKey)
                        end
                        redis.call('DEL',idxKey)
                        return cnt
                        """
        );
        return script;
    }

    /**
     * KYES[1] : refresh:{userId}:{sid}
     * ARGV[1] : expectedHash
     * ARGV[2] : newHash
     * ARGV[3] : ttlMillis
     * @return
     */
    @Bean("refreshCompareRotateScript")
    public DefaultRedisScript<Long> refreshCompareRotateScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                """
                        local cur = redis.call('GET',KEYS[1])
                        if not cur then
                            return 0
                        end
                        if cur ~= ARGV[1] then
                            return -1
                        end
                        redis.call('PSETEX',KEYS[1],ARGV[3],ARGV[2])
                            return 1
                        """
        );
        return script;
    }

}
