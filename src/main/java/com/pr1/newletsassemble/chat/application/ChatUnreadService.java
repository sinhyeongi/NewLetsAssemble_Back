package com.pr1.newletsassemble.chat.application;

import com.pr1.newletsassemble.chat.infra.redis.repository.ChatRedisRepository;
import com.pr1.newletsassemble.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatUnreadService {
    private final ChatRedisRepository redis;
    private final TimeProvider time;

    public Map<Long,Long> getUnreadSummary(long userId, List<Long> partyIds){
        if(userId <=0 || partyIds == null || partyIds.isEmpty()){return Map.of();}
        List<Long> filtered = partyIds.stream().filter( x -> x != null&& x > 0).toList();
        if(filtered.isEmpty()) return Map.of();
        Map<Long,Long> fresh = redis.getUnreadFresh(userId);
        if(!fresh.isEmpty()) return filterOnly(fresh,filtered);
        Map<Long,Long> stale = redis.getUnreadStale(userId);

        if(!redis.tryAcquireLeaderLock(userId)){
            return !stale.isEmpty() ? filterOnly(stale,filtered) : zeros(filtered);
        }

        Optional<Map<Long,Long>> computed = redis.computeWithTimeout(()-> computeUnread(userId,filtered));
        if(computed.isEmpty()){
            return !stale.isEmpty() ? filterOnly(stale,filtered) : zeros(filtered);
        }
        Map<Long,Long> result = computed.get();
        try{
            redis.writeUnreadWriteThenSwap(userId,result,time.now());
        }catch(Exception e){

        }
        return filterOnly(result,filtered);
    }

    private Map<Long,Long> computeUnread(long userId, List<Long> partyIds){
        Map<Long,Long> lastSeq = redis.getPartyLastSeqBatch(partyIds);
        Map<Long,Long> lastRead = redis.getUserLastReadSeqBatch(userId,partyIds);
        Map<Long,Long> out = new HashMap<>(partyIds.size());
        for(Long pid : partyIds){
            long ls = lastSeq.getOrDefault(pid,0L);
            long lr = lastRead.getOrDefault(pid,0L);
            out.put(pid,Math.max(0L,(ls - lr )));
        }
        return out;
    }
    private Map<Long,Long> filterOnly(Map<Long,Long> src, List<Long> partyIds){
        Map<Long,Long> out = new HashMap<>();
        for(long pid : partyIds){
            out.put(pid,src.getOrDefault(pid,0L));
        }
        return out;
    }
    private Map<Long,Long> zeros(List<Long> partyIds){
        Map<Long,Long> out = new HashMap<>();
        for(long pid : partyIds){
            out.put(pid,0L);
        }
        return out;
    }
}
