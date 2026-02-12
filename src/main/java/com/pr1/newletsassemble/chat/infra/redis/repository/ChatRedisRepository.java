package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisKeys;
import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisTtl;
import com.pr1.newletsassemble.global.time.TimeProvider;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Repository
@Slf4j
public class ChatRedisRepository {

    private final StringRedisTemplate redis;

    private final DefaultRedisScript<Long> presenceTouchScript;
    private final DefaultRedisScript<Long> presenceCleanupScript;
    private final DefaultRedisScript<Long> zsetMaxUpdateScript;

    private final DefaultRedisScript<Long> dirtyMarkScript;
    private final DefaultRedisScript<List> dirtyClaimDueUsersScript;
    private final DefaultRedisScript<List> dirtyRequeueExpiredProcessingScript;
    private final DefaultRedisScript<List> dirtyFetchPartiesScript;
    private final DefaultRedisScript<Long> dirtyAckScript;
    private final DefaultRedisScript<Long> dirtyNackScript;
    private final DefaultRedisScript<Long> unreadSwapWithNonceScript;

    private final ExecutorService leaderComputePool =
            Executors.newFixedThreadPool(4, r ->{
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("chat-unread-leader");
                return t;
            });
    private final TimeProvider timeProvider;

    @PreDestroy
    public void shutdown(){
        leaderComputePool.shutdownNow();
    }

    public ChatRedisRepository(StringRedisTemplate redis,
                               @Qualifier("presenceTouchScript")DefaultRedisScript<Long> presenceTouchScript,
                               @Qualifier("presenceCleanupScript")DefaultRedisScript<Long> presenceCleanupScript,
                               @Qualifier("zsetMaxUpdateScript")DefaultRedisScript<Long> zsetMaxUpdateScript,
                               @Qualifier("dirtyMarkScript")DefaultRedisScript<Long> dirtyMarkScript,
                               @Qualifier("dirtyClaimDueUsersScript")DefaultRedisScript<List> dirtyClaimDueUsersScript,
                               @Qualifier("dirtyRequeueExpiredProcessingScript")DefaultRedisScript<List> dirtyRequeueExpiredProcessingScript,
                               @Qualifier("dirtyFetchPartiesScript")DefaultRedisScript<List> dirtyFetchPartiesScript,
                               @Qualifier("dirtyAckScript")DefaultRedisScript<Long> dirtyAckScript,
                               @Qualifier("dirtyNackScript")DefaultRedisScript<Long> dirtyNackScript,
                               @Qualifier("unreadSwapWithNonceScript")DefaultRedisScript<Long> unreadSwapWithNonceScript,
                               TimeProvider timeProvider){
        this.redis = redis;
        this.presenceTouchScript = presenceTouchScript;
        this.presenceCleanupScript = presenceCleanupScript;
        this.zsetMaxUpdateScript = zsetMaxUpdateScript;
        this.dirtyMarkScript = dirtyMarkScript;
        this.dirtyClaimDueUsersScript = dirtyClaimDueUsersScript;
        this.dirtyRequeueExpiredProcessingScript = dirtyRequeueExpiredProcessingScript;
        this.dirtyFetchPartiesScript = dirtyFetchPartiesScript;
        this.dirtyAckScript = dirtyAckScript;
        this.dirtyNackScript = dirtyNackScript;
        this.unreadSwapWithNonceScript = unreadSwapWithNonceScript;
        this.timeProvider = timeProvider;
    }
    /* ------------- party last seq ------------- */
    public long incrPartyLastSeq(long partyId){
        Long v = redis.opsForValue().increment(ChatRedisKeys.partyLastSeq(partyId));
        return v == null ? 0L : v;
    }
    public long getPartyLastSeq(long partyId){
        String v = redis.opsForValue().get(ChatRedisKeys.partyLastSeq(partyId));
        if(v == null){return 0L;}
        try{
            return Long.parseLong(v);
        }catch(Exception e){
            log.warn("lastSeq parse Failed partyId = {}",partyId,e);
            return 0L;
        }
    }
    public Map<Long,Long> getPartyLastSeqBatch(List<Long> partyIds){
        if(partyIds == null || partyIds.isEmpty()) return Map.of();
        List<Long> filtered = new ArrayList<>();
        for(Long pid : partyIds){
            if(pid != null && pid > 0 ){
                filtered.add(pid);
            }
        }
        if(filtered.isEmpty()){ return Map.of(); }
        List<Object> res = redis.executePipelined((RedisCallback<Object>) con -> {
            for(Long pid : filtered){
                con.commands().get(ChatRedisKeys.partyLastSeq(pid).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
        Map<Long,Long> out = new HashMap<>();
        for(int i=0; i < filtered.size(); i++){
            Long pid = filtered.get(i);
            Object raw = res.get(i);
            if(raw == null){
                out.put(pid,0L);
                continue;
            }
            if(raw instanceof byte[] b){
                try{
                    out.put(pid,Long.parseLong(new String(b,StandardCharsets.UTF_8)));
                }catch(Exception ignore){
                    out.put(pid,0L);
                }
            }else{
                out.put(pid,0L);
            }
        }
        return out;
    }
    /* ------------- members (SET) ------------- */
    public Set<Long> getPartyMembers(long partyId){
        Set<String> s = redis.opsForSet().members(ChatRedisKeys.partyMembers(partyId));
        return validSetAndParseLong(s);
    }
    public void addPartyMember(long partyId,long userId){
        try {
            Long v = redis.opsForSet().add(ChatRedisKeys.partyMembers(partyId), String.valueOf(userId));
            if(v == null){

            }
        }catch(Exception ignored){

        }
    }
    public void removePartyMember(long partyId,long userId){
        try {
            Long v = redis.opsForSet().remove(ChatRedisKeys.partyMembers(partyId), String.valueOf(userId));
            if (v == null) {

            }
        }catch(Exception e){

        }
    }
    /* ------------- presence (ZSET expireAt score) ------------- */
    public boolean touchPresence(long partyId, long userId, Instant now){
        long expAt = now.plus(ChatRedisTtl.PRESENCE_TTL).toEpochMilli();
        Long r = redis.execute(
          presenceTouchScript,
          List.of(ChatRedisKeys.partyPresence(partyId)),
          String.valueOf(userId),
          String.valueOf(expAt)
        );
        return r != null && r == 1L;
    }
    public void cleanupPresence(long partyId,Instant now){
        try{
            redis.execute(presenceCleanupScript,
                    List.of(ChatRedisKeys.partyPresence(partyId)),
                    String.valueOf(now.toEpochMilli())
            );
        }catch(Exception ignored){
            log.warn("presence cleanup failed partyId={} ",partyId,ignored);
        }
    }

    /* ------------- user read seq (ZSET Lua max update) ------------- */
    public long getUserLastReadSeq(long partyId, long userId){
        Double score = redis.opsForZSet().score(ChatRedisKeys.userReadSeq(userId),String.valueOf(partyId));
        return score == null ? 0L : score.longValue();
    }
    public Map<Long,Long> getUserLastReadSeqBatch(long userId,List<Long> partyIds){
        if(partyIds == null || partyIds.isEmpty()) {return Map.of();}
        List<Long> filtered = new ArrayList<>();
        for(Long pid : partyIds){
            if(pid != null && pid > 0 ){
                filtered.add(pid);
            }
        }
        if(filtered.isEmpty()){return Map.of();}

        byte[] zkey = ChatRedisKeys.userReadSeq(userId).getBytes(StandardCharsets.UTF_8);
        List<Object> res = redis.executePipelined((RedisCallback<Object>) con ->{
            for(Long pid : filtered){
                con.commands().zScore(zkey,String.valueOf(pid).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        Map<Long,Long> out = new HashMap<>();
        for(int i=0; i < filtered.size(); i++){
            Long pid = filtered.get(i);
            Object raw = res.get(i);
            if(raw == null) {out.put(pid,0L); continue;}
            if(raw instanceof Double b){
                out.put(pid,b.longValue());
            }else if(raw instanceof byte[] b){
                try{
                    out.put(pid,(long)Double.parseDouble(new String(b,StandardCharsets.UTF_8)));
                }catch(Exception ignore){out.put(pid,0L);}
            }else{
                try {
                    out.put(pid, Long.parseLong(String.valueOf(raw)));
                }catch(Exception ignored){out.put(pid,0L);}
            }
        }
        return out;
    }
    public boolean updateUserLastReadSeqMax(long userId, long partyId, long newSeq){
        Long r = redis.execute(zsetMaxUpdateScript,
                List.of(ChatRedisKeys.userReadSeq(userId)),
                String.valueOf(partyId),
                String.valueOf(newSeq)
        );
        return r != null && r == 1L;
    }

    /* ------------- unread cache (HASH fresh + stale) ------------- */

    private Map<Long,Long> readUnreadHash(String key){
        Map<Object, Object> raw = redis.opsForHash().entries(key);
        if(raw == null || raw.isEmpty()) return Map.of();
        Map<Long,Long> out = new HashMap<>(raw.size());
        for(var e : raw.entrySet()){
            try{
                long partyId = Long.parseLong(String.valueOf(e.getKey()));
                long cnt = Long.parseLong(String.valueOf(e.getValue()));
                out.put(partyId,Math.max(0L,cnt));
            }catch(Exception ignore){}
        }
        return out;
    }
    public Map<Long,Long> getUnreadFresh(long userId){
        return readUnreadHash(ChatRedisKeys.unreadFresh(userId));
    }
    public Map<Long,Long> getUnreadStale(long userId){
        return readUnreadHash(ChatRedisKeys.unreadStale(userId));
    }
    public boolean writeUnreadWriteThenSwap(long userId,Map<Long,Long> unread,Instant now){
        if(unread == null ) unread = Map.of();
        String freshKey = ChatRedisKeys.unreadFresh(userId);
        String staleKey = ChatRedisKeys.unreadStale(userId);
        String nonceKey = now.toEpochMilli() + "-" + ThreadLocalRandom.current().nextInt(1_000_000);
        long nonce = Optional.ofNullable(redis.opsForValue().increment(ChatRedisKeys.unreadNonceSeq(userId))).orElse(1L);
        String tmpFreshKey = ChatRedisKeys.unreadFreshTmp(userId,nonceKey);
        String tmpStaleKey = ChatRedisKeys.unreadStaleTmp(userId,nonceKey);

        long freshSec = ChatRedisTtl.UNREAD_FRESH_TTL.getSeconds();
        long staleSec = ChatRedisTtl.UNREAD_STALE_TTL.getSeconds();
        long tmpTTL = ChatRedisTtl.UNREAD_TMP_TTL.getSeconds();

        Map<String,String> payload = new HashMap<>();
        for(var e : unread.entrySet()){
            payload.put(String.valueOf(e.getKey()),String.valueOf(Math.max(0L,e.getValue())));
        }
        try {
            redis.executePipelined((RedisCallback<Object>) con -> {
                byte[] tf = tmpFreshKey.getBytes(StandardCharsets.UTF_8);
                byte[] ts = tmpStaleKey.getBytes(StandardCharsets.UTF_8);

                con.commands().del(tf);
                con.commands().del(ts);

                if (!payload.isEmpty()) {
                    for (var e : payload.entrySet()) {
                        byte[] f = e.getKey().getBytes(StandardCharsets.UTF_8);
                        byte[] v = e.getValue().getBytes(StandardCharsets.UTF_8);
                        con.commands().hSet(tf, f, v);
                        con.commands().hSet(ts, f, v);
                    }
                }
                con.commands().expire(tf, tmpTTL);
                con.commands().expire(ts, tmpTTL);
                return null;
            });
        }catch(Exception e){
            return false;
        }
        Long swap = redis.execute(
                unreadSwapWithNonceScript,
                List.of(
                        freshKey,staleKey,tmpFreshKey,tmpStaleKey,ChatRedisKeys.unreadNonceApplied(userId)
                ),
                String.valueOf(nonce),
                String.valueOf(freshSec),
                String.valueOf(staleSec)
        );

        return swap != null && swap == 1L;
    }

    /* ------------- dirty (claim/ack/nack) ------------- */
    public boolean markDirtyUserParty(long userId, long partyId, Duration delay){
        long nextAt = timeProvider.now().plus(delay).toEpochMilli();
        long ttlMs = ChatRedisTtl.DIRTY_SET_TTL.toMillis();
        Long r = redis.execute(
                dirtyMarkScript,
                List.of(ChatRedisKeys.dirtyUsers(),ChatRedisKeys.dirtyUserParties(userId)),
                String.valueOf(userId),
                String.valueOf(partyId),
                String.valueOf(nextAt),
                String.valueOf(ttlMs)
        );
        return r != null && r == 1L;
    }
    public List<String> claimDueDirtyUsers(int limit){
        long now = timeProvider.now().toEpochMilli();
        Object raw = redis.execute(
                dirtyClaimDueUsersScript,
                List.of(ChatRedisKeys.dirtyUsers(),ChatRedisKeys.dirtyProcessing()),
                String.valueOf(now),
                String.valueOf(limit),
                String.valueOf(ChatRedisTtl.DIRTY_PROCESSING_LEASE.toMillis())
        );
        return castStringList(raw);
    }
    public List<String> requeueExpiredProcessing(int limit){
        long now = timeProvider.now().toEpochMilli();
        Object raw = redis.execute(
                dirtyRequeueExpiredProcessingScript,
                List.of(ChatRedisKeys.dirtyUsers(),ChatRedisKeys.dirtyProcessing()),
                String.valueOf(now),
                String.valueOf(limit)
        );
        return castStringList(raw);
    }
    public List<String> fetchDirtyParties(long userId){
        Object raw = redis.execute(
                dirtyFetchPartiesScript,
                List.of(ChatRedisKeys.dirtyUserParties(userId))
        );
        return castStringList(raw);
    }
    public boolean ackDirty(long userId){
        Long r = redis.execute(
                dirtyAckScript,
                List.of(ChatRedisKeys.dirtyProcessing(), ChatRedisKeys.dirtyUserParties(userId), ChatRedisKeys.dirtyRetry(userId)),
                String.valueOf(userId)
        );
        return r != null && r == 1L;
    }
    public boolean nackDirty(long userId, long nextRetryAtMillis){
        long ttlMs = ChatRedisTtl.DIRTY_PARTIES_TTL.toMillis();
        Long r = redis.execute(
                dirtyNackScript,
                List.of(ChatRedisKeys.dirtyUsers(),ChatRedisKeys.dirtyProcessing(),ChatRedisKeys.dirtyUserParties(userId)),
                String.valueOf(userId),
                String.valueOf(nextRetryAtMillis),
                String.valueOf(ttlMs)
        );
        return r != null && r == 1L;
    }
    public long incrDirtyRetry(long userId){
        String key = ChatRedisKeys.dirtyRetry(userId);
        Long v = redis.opsForValue().increment(key);
        redis.expire(key,ChatRedisTtl.DIRTY_RETRY);
        return v == null ? 0L : v;
    }
    public void resetDirtyRetry(long userId){
        redis.delete(ChatRedisKeys.dirtyRetry(userId));
    }
    /* ------------- singleflight leader lock ------------- */
    public boolean tryAcquireLeaderLock(long userId){
        String key = ChatRedisKeys.unreadLeaderLock(userId);
        return Boolean.TRUE.equals(
                redis.execute((RedisCallback<Boolean>) con ->
                    con.commands().set(
                            key.getBytes(StandardCharsets.UTF_8),
                            "1".getBytes(StandardCharsets.UTF_8),
                            Expiration.milliseconds(ChatRedisTtl.LEADER_LOCK_TTL.toMillis()),
                            RedisStringCommands.SetOption.SET_IF_ABSENT
                    )
                )
        );
    }
    public Optional<Map<Long,Long>> computeWithTimeout(Callable<Map<Long,Long>> job){
        Future<Map<Long,Long>> f = leaderComputePool.submit(job);
        try{
            return Optional.ofNullable(
                    f.get(ChatRedisTtl.LEADER_COMPUTE_TIMEOUT.toMillis(),TimeUnit.MILLISECONDS)
            );
        }catch(TimeoutException te){
            f.cancel(true);
            return Optional.empty();
        }catch(Exception e){
            f.cancel(true);
            return Optional.empty();
        }

    }
    /* ------------- flush lock ------------- */
    public boolean tryAcquireFlushLock(long userId){
        String key = ChatRedisKeys.flushLock(userId);
        return Boolean.TRUE.equals(
                redis.execute((RedisCallback<Boolean>) con ->
                con.commands().set(
                        key.getBytes(StandardCharsets.UTF_8),
                        "1".getBytes(StandardCharsets.UTF_8),
                        Expiration.milliseconds(ChatRedisTtl.FLUSH_LOCK_TTL.toMillis()),
                        RedisStringCommands.SetOption.SET_IF_ABSENT
                )
                               )
        );
    }
    /* ------------- helper (SET) ------------- */
    private Set<Long> validSetAndParseLong(Set<String> s){
        if(s == null || s.isEmpty()){return Set.of();}
        Set<Long> out = new HashSet<>();
        for(String v : s){
            try{
                out.add(Long.parseLong(v));
            }catch(Exception ignore){

            }
        }
        return out;
    }
    private List<String> castStringList(Object raw){
        if(raw == null) return List.of();
        if(raw instanceof List<?> list){
            List<String> out = new ArrayList<>();
            for(Object o : list){
                if( o == null) continue;
                try {
                    out.add(String.valueOf(o));
                }catch(Exception ignore){

                }
            }
            return out;
        }
        return List.of();
    }
}
