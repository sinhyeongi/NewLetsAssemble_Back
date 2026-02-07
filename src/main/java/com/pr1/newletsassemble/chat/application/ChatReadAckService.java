package com.pr1.newletsassemble.chat.application;

import com.pr1.newletsassemble.chat.infra.redis.keys.ChatRedisTtl;
import com.pr1.newletsassemble.chat.infra.redis.repository.ChatRedisRepository;
import com.pr1.newletsassemble.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReadAckService {
    private final ChatRedisRepository redis;
    private final TimeProvider time;
    @Transactional
    public void ackRead(long userId,long partyId, long lastReadSeq){
        if(userId <= 0 || partyId <= 0 || lastReadSeq <= 0){return;}
        boolean res = redis.updateUserLastReadSeqMax(userId,partyId,lastReadSeq);
        if(res){
            redis.markDirtyUserParty(userId,partyId, ChatRedisTtl.DIRTY_USER_PARTIES_TTL);
        }
    }
}
