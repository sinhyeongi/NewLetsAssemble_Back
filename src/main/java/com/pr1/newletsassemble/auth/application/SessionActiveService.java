package com.pr1.newletsassemble.auth.application;

import com.pr1.newletsassemble.auth.infra.redis.key.RedisKeys;
import com.pr1.newletsassemble.auth.infra.redis.repository.SessionActiveCacheRepository;
import com.pr1.newletsassemble.auth.infra.session.jdbc.dao.DeviceSessionActiveUpsertDao;
import com.pr1.newletsassemble.auth.infra.session.jpa.repository.DeviceSessionActiveJpaRepository;
import com.pr1.newletsassemble.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionActiveService {
    private final DeviceSessionActiveJpaRepository activeJpa; // 조회 / 삭제용
    private final DeviceSessionActiveUpsertDao upsertDao; // upsert용 ( 원자성 / 성능 )
    private final SessionActiveCacheRepository cache;
    private final TimeProvider time;

    /**
     *  refresh만료 + 유예기간
     */
    public Duration cacheTtl(Duration refreshTtl){
        return refreshTtl.plusDays(7);
    }

    public String upsertActiveAndGetOldSid(long userId, String deviceKey, String newSid,Duration ttl){
        Instant now = time.now();
        String oldSid = upsertDao.upsertAndReturnOldSid(userId,deviceKey,newSid,now);
        runAfterCommit(()->{
           try{
                cache.set(RedisKeys.sessionActive(userId,deviceKey),newSid,cacheTtl(ttl));
           }catch(Exception ignored){}
        });
        return oldSid;
    }

    public Optional<String> getActiveSid(long userId,String deviceKey, Duration refreshTtl){
        String key = RedisKeys.sessionActive(userId,deviceKey);
        try{
            Optional<String> hit = cache.get(key);
            if(hit.isPresent()){return hit;}
        }catch(Exception ignored){}
        Optional<String> db = activeJpa.findSessionIdByUserIdAndDeviceKey(userId,deviceKey);
        db.ifPresent(sid -> {
            runAfterCommitOrNow(() ->{
               try{
                   cache.set(key,sid,cacheTtl(refreshTtl));
               }catch(Exception ignored){}
            });
        });
        return db;
    }
    private void runAfterCommit(Runnable r){
        if(TransactionSynchronizationManager.isActualTransactionActive()){
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    r.run();
                }
            });
        }else{
            r.run();
        }
    }

    private void runAfterCommitOrNow(Runnable r){
        runAfterCommit(r);
    }
}
