package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ChatPartySeqRedisRepository {
    private final StringRedisTemplate redis;

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
}
