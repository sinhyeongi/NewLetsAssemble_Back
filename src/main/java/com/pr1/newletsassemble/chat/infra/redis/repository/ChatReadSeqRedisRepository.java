package com.pr1.newletsassemble.chat.infra.redis.repository;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisKeys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ChatReadSeqRedisRepository {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> zsetMaxUpdateScript;

    public ChatReadSeqRedisRepository(
            StringRedisTemplate redis,
            @Qualifier("zsetMaxUpdateScript")DefaultRedisScript<Long> zsetMaxUpdateScript) {
        this.redis = redis;
        this.zsetMaxUpdateScript = zsetMaxUpdateScript;
    }

    public long getUserLastReadSeq(long partyId, long userId){
        Double score = redis.opsForZSet().score(ChatRedisKeys.userReadSeq(userId),String.valueOf(partyId));
        return score == null ? 0L : score.longValue();
    }

    public Map<Long,Long> getUserLastReadSeqBatch(long userId, List<Long> partyIds){
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
}
