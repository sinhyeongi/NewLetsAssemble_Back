package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisKeys;
import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisTtl;
import com.pr1.newletsassemble.global.time.TimeProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ChatDirtyQueueRedisRepository {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> dirtyMarkScript;
    private final DefaultRedisScript<List> dirtyClaimDueUsersScript;
    private final DefaultRedisScript<List> dirtyRequeueExpiredProcessingScript;
    private final DefaultRedisScript<List> dirtyFetchPartiesScript;
    private final DefaultRedisScript<Long> dirtyAckScript;
    private final DefaultRedisScript<Long> dirtyNackScript;
    private final TimeProvider timeProvider;
    public ChatDirtyQueueRedisRepository(StringRedisTemplate redis,
                                         @Qualifier("dirtyMarkScript")DefaultRedisScript<Long> dirtyMarkScript,
                                         @Qualifier("dirtyClaimDueUsersScript") DefaultRedisScript<List> dirtyClaimDueUsersScript,
                                         @Qualifier("dirtyRequeueExpiredProcessingScript") DefaultRedisScript<List> dirtyRequeueExpiredProcessingScript,
                                         @Qualifier("dirtyFetchPartiesScript") DefaultRedisScript<List> dirtyFetchPartiesScript,
                                         @Qualifier("dirtyAckScript") DefaultRedisScript<Long> dirtyAckScript,
                                         @Qualifier("dirtyNackScript") DefaultRedisScript<Long> dirtyNackScript,
                                         TimeProvider timeProvider){
        this.redis = redis;
        this.dirtyMarkScript = dirtyMarkScript;
        this.dirtyClaimDueUsersScript = dirtyClaimDueUsersScript;
        this.dirtyRequeueExpiredProcessingScript = dirtyRequeueExpiredProcessingScript;
        this.dirtyFetchPartiesScript = dirtyFetchPartiesScript;
        this.dirtyAckScript = dirtyAckScript;
        this.dirtyNackScript = dirtyNackScript;
        this.timeProvider = timeProvider;
    }
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
