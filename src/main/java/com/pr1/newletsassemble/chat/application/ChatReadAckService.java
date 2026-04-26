package com.pr1.newletsassemble.chat.application;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisTtl;
import com.pr1.newletsassemble.chat.infra.redis.repository.ChatDirtyQueueRedisRepository;
import com.pr1.newletsassemble.chat.infra.redis.repository.ChatReadSeqRedisRepository;
import com.pr1.newletsassemble.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReadAckService {
    private final ChatReadSeqRedisRepository readSeqRedis;
    private final ChatDirtyQueueRedisRepository dirtyQueueRedis;
    private final TimeProvider time;
    @Transactional
    public void ackRead(long userId,long partyId, long lastReadSeq){
        if(userId <= 0 || partyId <= 0 || lastReadSeq <= 0){return;}
        boolean res = readSeqRedis.updateUserLastReadSeqMax(userId,partyId,lastReadSeq);
        if(res){
            dirtyQueueRedis.markDirtyUserParty(userId,partyId, ChatRedisTtl.DIRTY_USER_PARTIES_TTL);
        }
    }
}
