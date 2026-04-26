package com.pr1.newletsassemble.chat.application.port.out;

public interface ChatReadPort {
    long getLastReadSeq(Long partyId,Long userId);
    void updateLastReadSeq(Long partyId,Long userId,long seq);

}
