package com.pr1.newletsassemble.chat.infra.persistence.jpa;

import com.pr1.newletsassemble.chat.domain.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface ChatJpaRepository extends JpaRepository<Chat,Long> {
    Optional<Chat> findByParty_IdAndSender_IdAndClientMessageId(Long partyId, Long senderId, String clientMessageId);
    @Query("""
        select c
        from Chat c
            where c.party.id = :partyId
                order by c.id desc
    """)
    List<Chat> findRecent(Long partyId, Pageable pageable);

    @Query("""
            select c
            from Chat c
            where c.party.id = :partyId
            and  c.id < :beforeChatId
                order by c.id desc
    """)
    List<Chat> findBefore(Long partyId, Long beforeChatId, Pageable pageable);

}
