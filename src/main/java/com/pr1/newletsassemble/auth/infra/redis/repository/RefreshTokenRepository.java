package com.pr1.newletsassemble.auth.infra.redis.repository;

import com.pr1.newletsassemble.auth.infra.session.RefreshTokenRotateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
public class RefreshTokenRepository {
    private final StringRedisTemplate redis;

    private final DefaultRedisScript<Long> saveScript;
    private final DefaultRedisScript<Long> deleteOneScript;
    private final DefaultRedisScript<Long> deleteAllScript;
    private final DefaultRedisScript<Long> compareRotateScript;
    public RefreshTokenRepository(StringRedisTemplate redis,
                                  @Qualifier("refreshSaveScript")  DefaultRedisScript<Long> saveScript,
                                  @Qualifier("refreshDeleteOneScript") DefaultRedisScript<Long> deleteOneScript,
                                  @Qualifier("refreshDeleteAllScript") DefaultRedisScript<Long> deleteAllScript,
                                  @Qualifier("refreshCompareRotateScript") DefaultRedisScript<Long> compareRotateScript) {
        this.redis = redis;
        this.saveScript = saveScript;
        this.deleteOneScript = deleteOneScript;
        this.deleteAllScript = deleteAllScript;
        this.compareRotateScript = compareRotateScript;
    }
    private String tokenKey(Long userId,String sid){
        return "refresh:" + userId + ":" + sid;
    }
    private String indexKey(Long userId){
        return "refresh:idx:" + userId;
    }
    /* =================== Save ( Atomic ) ================== */
    public boolean saveHashAtomic(Long userId, String sid, String hash, Duration ttl){
        long ttlMillis = ttl.toMillis();
        Long result = redis.execute(
                saveScript,
                List.of(tokenKey(userId,sid),indexKey(userId)),
                hash,
                String.valueOf(ttlMillis),
                sid
        );
        return result != null && result == 1L;
    }
    /* =================== Load ================== */
    public String loadHash(Long userId, String sid){
        return redis.opsForValue().get(tokenKey(userId,sid));
    }
    /* =================== DeleteOne ( Atomic ) ================== */
    public boolean deleteOneAtomic(Long userId, String sid){
        Long result = redis.execute(
                deleteOneScript,
                List.of(tokenKey(userId,sid),indexKey(userId)),
                sid
        );
        return result != null && result == 1L;
    }
    /* =================== DeleteAll ( Atomic ) ================== */
    public long deleteAllAtomic(Long userId){
        Long result = redis.execute(
                deleteAllScript,
                List.of(indexKey(userId)),
                String.valueOf(userId)
        );
        return result == null ? 0L : result;
    }
    public RefreshTokenRotateResult rotate(Long userId, String sid, String oldHash, String newHash, Duration ttl){
        return RefreshTokenRotateResult.from(
                redis.execute(compareRotateScript,
                List.of(tokenKey(userId,sid))
                ,oldHash,
                newHash,
                String.valueOf(ttl.toMillis())
                )
        );
    }
}
