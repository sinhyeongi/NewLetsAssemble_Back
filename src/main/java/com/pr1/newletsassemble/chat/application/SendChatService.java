package com.pr1.newletsassemble.chat.application;

import com.pr1.newletsassemble.chat.application.port.in.SendChatUseCase;
import com.pr1.newletsassemble.chat.application.port.out.ChatPersistPort;
import com.pr1.newletsassemble.chat.application.port.out.ChatPublisherPort;
import com.pr1.newletsassemble.chat.application.port.out.ChatSeqPort;
import com.pr1.newletsassemble.chat.application.port.out.PartyMemberReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class SendChatService implements SendChatUseCase {
    private final ChatPublisherPort publisherPort;
    private final ChatPersistPort persistPort;
    private final PartyMemberReaderPort partyMemberReaderPort;
    private final ChatSeqPort chatSeqport;

    @Transactional
    @Override
    public void send(Long roomId, Long userId, String content) {

    }
}
