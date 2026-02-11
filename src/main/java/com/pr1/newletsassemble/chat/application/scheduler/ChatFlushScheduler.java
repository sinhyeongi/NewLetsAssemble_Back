package com.pr1.newletsassemble.chat.application.scheduler;

import com.pr1.newletsassemble.chat.infra.persistence.jdbc.ChatPartyMemberJdbcRepository;
import com.pr1.newletsassemble.chat.infra.persistence.jdbc.dto.Row;
import com.pr1.newletsassemble.chat.infra.redis.repository.ChatRedisRepository;
import com.pr1.newletsassemble.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatFlushScheduler {
    private final ChatRedisRepository redis;
    private final ChatPartyMemberJdbcRepository jdbc;
    private final TimeProvider time;
    private final ChatSchedulerProperties props;

    @Scheduled(fixedDelayString = "30000")
    public void flushReadSeqToDb(){
        List<String> dueUsers = redis.popDueDirtyUsers(props.getScheduleLimit());
        if(dueUsers.isEmpty()) return;
        for(String userStr : dueUsers){
            long userId;
            try{
                userId = Long.parseLong(userStr);
            }catch(Exception e){
                continue;
            }
            if(!redis.tryAcquireFlushLock(userId)){
                continue;
            }
            try{
                List<String> parties = redis.fetchAndClearDirtyParties(userId);
                if(parties.isEmpty()){
                    redis.resetDirtyRetry(userId);
                    continue;
                }
                List<Long> partyIds = new ArrayList<>(parties.size());
                for(String p  : parties){
                    try{
                        long pid = Long.parseLong(p);
                        if(pid > 0){ partyIds.add(pid);}
                    }catch(Exception e){

                    }
                }
                if(partyIds.isEmpty()){
                    redis.resetDirtyRetry(userId);
                    continue;
                }
                Map<Long,Long>lastReadMap = redis.getUserLastReadSeqBatch(userId,partyIds);
                List<Row> rows = new ArrayList<>(partyIds.size());
                for(Long pid : partyIds){
                    long lastRead = lastReadMap.getOrDefault(pid,0L);
                    rows.add(new Row(userId,pid,lastRead));
                }
                jdbc.batchUpdateLastRead(rows,time.now());
                redis.resetDirtyRetry(userId);

            }catch(Exception e){
                long retry = redis.incrDirtyRetry(userId);

            }
        }

    }

}
