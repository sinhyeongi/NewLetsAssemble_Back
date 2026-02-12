package com.pr1.newletsassemble.chat.application.scheduler;

import com.pr1.newletsassemble.chat.infra.persistence.jdbc.ChatPartyMemberJdbcRepository;
import com.pr1.newletsassemble.chat.infra.persistence.jdbc.dto.Row;
import com.pr1.newletsassemble.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatFlushScheduler {
    private final ChatRedisRepository redis;
    private final ChatPartyMemberJdbcRepository jdbc;
    private final TimeProvider time;
    private final ChatSchedulerProperties props;

    @Scheduled(fixedDelayString = "30000")
    public void flushReadSeqToDb(){
        long now = time.now().toEpochMilli();
        redis.requeueExpiredProcessing(200);

        List<String> users = redis.claimDueDirtyUsers(props.getScheduleLimit());
        if(users.isEmpty()) return;

        for(String u : users){
            long userId;
            try{
                userId = Long.parseLong(u);
            }catch(Exception e){
                continue;
            }
            if (!redis.tryAcquireFlushLock(userId)) {
                continue;
            }
            try{
                List<String> partyStrs = redis.fetchDirtyParties(userId);
                if(partyStrs.isEmpty()){
                    redis.ackDirty(userId);
                    continue;
                }
                List<Long> partyIds = new ArrayList<>(partyStrs.size());
                for(String s : partyStrs){
                    try{
                        long pid = Long.parseLong(s);
                        if(pid > 0) partyIds.add(pid);
                    }catch(Exception e){}
                }
                if(partyIds.isEmpty()){
                    redis.ackDirty(userId);
                    continue;
                }
                var readMap = redis.getUserLastReadSeqBatch(userId,partyIds);
                List<Row> rows = new ArrayList<>(partyIds.size());
                for(Long pid : partyIds){
                    long seq = readMap.getOrDefault(pid,0L);
                    rows.add(new Row(userId,pid,seq));
                }
                jdbc.batchUpdateLastRead(rows,time.now());
                redis.ackDirty(userId);
            }catch(Exception e){
                long retry = redis.incrDirtyRetry(userId);
                long backoffMs = Math.min(60_000L,1500L * retry);
                long nextAt = time.now().toEpochMilli() + backoffMs;

                redis.nackDirty(userId,nextAt);
            }
        }

    }

}
