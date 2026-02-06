package com.pr1.newletsassemble.party.infra.persistence.jpa;

import com.pr1.newletsassemble.party.domain.member.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface PartyMemberJpaRepository extends JpaRepository<PartyMember, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update PartyMember pm
        set pm.lastReadSeq = case when :seq > pm.lastReadSeq then :seq else pm.lastReadSeq end,
            pm.lastReadAt  = case when :seq > pm.lastReadSeq then :now else pm.lastReadAt end
        where pm.party.id = :partyId
            and pm.user.id = :userId
    """)
    int advanceRead(
            @Param("partyId") long partyId,
            @Param("userId")long userId,
            @Param("seq")long seq,
            @Param("now")Instant now
    );
}
