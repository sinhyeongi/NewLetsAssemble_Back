package com.pr1.newletsassemble.chat.application.port.out;

import java.util.List;
import java.util.Map;

public interface ChatSeqPort {
    long nextSeq(Long partyId);
    long currentSeq(Long partyId);
    Map<Long,Long> getCurrentSeqBatch(List<Long> partyIds);
}
