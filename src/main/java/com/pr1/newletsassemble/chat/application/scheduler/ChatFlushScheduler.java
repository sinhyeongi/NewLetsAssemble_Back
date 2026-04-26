package com.pr1.newletsassemble.chat.application.scheduler;

import com.pr1.newletsassemble.chat.application.scheduler.properties.ChatSchedulerProperties;
import com.pr1.newletsassemble.chat.infra.persistence.jdbc.ChatPartyMemberJdbcRepository;
import com.pr1.newletsassemble.chat.infra.persistence.jdbc.dto.Row;
import com.pr1.newletsassemble.chat.infra.redis.repository.ChatDirtyQueueRedisRepository;
import com.pr1.newletsassemble.chat.infra.redis.repository.ChatLockRepository;
import com.pr1.newletsassemble.chat.infra.redis.repository.ChatReadSeqRedisRepository;
import com.pr1.newletsassemble.chat.infra.redis.support.ChatLeaderComputeExecutor;
import com.pr1.newletsassemble.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatFlushScheduler {
    private final ChatLockRepository lockRepo;
    private final ChatDirtyQueueRedisRepository dirtyQueueRepo;
    private final ChatReadSeqRedisRepository readSeqRepo;
    private final ChatPartyMemberJdbcRepository jdbc;
    private final TimeProvider time;
    private final ChatSchedulerProperties props;

    @Scheduled(fixedDelayString = "30000")
    public void flushReadSeqToDb(){

        dirtyQueueRepo.requeueExpiredProcessing(200);

        List<String> users = dirtyQueueRepo.claimDueDirtyUsers(props.getScheduleLimit());
        if(users.isEmpty()) return;

        for(String u : users){
            long userId;
            try{
                userId = Long.parseLong(u);
            }catch(Exception e){
                continue;
            }
            if (!lockRepo.tryAcquireFlushLock(userId)) {
                continue;
            }
            try{
                List<String> partyStrs = dirtyQueueRepo.fetchDirtyParties(userId);
                if(partyStrs.isEmpty()){
                    dirtyQueueRepo.ackDirty(userId);
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
                    dirtyQueueRepo.ackDirty(userId);
                    continue;
                }
                var readMap = readSeqRepo.getUserLastReadSeqBatch(userId,partyIds);
                List<Row> rows = new ArrayList<>(partyIds.size());
                for(Long pid : partyIds){
                    long seq = readMap.getOrDefault(pid,0L);
                    rows.add(new Row(userId,pid,seq));
                }
                jdbc.batchUpdateLastRead(rows,time.now());
                dirtyQueueRepo.ackDirty(userId);
            }catch(Exception e){
                long retry = dirtyQueueRepo.incrDirtyRetry(userId);
                long backoffMs = Math.min(60_000L,1500L * retry);
                long nextAt = time.now().toEpochMilli() + backoffMs;

                dirtyQueueRepo.nackDirty(userId,nextAt);
            }
        }

    }

}
