package com.pr1.newletsassemble.chat.application.port.out;

public interface PartyMemberReaderPort {
    boolean exists(Long roomId,Long userId);
}
