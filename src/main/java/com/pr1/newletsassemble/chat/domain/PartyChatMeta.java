package com.pr1.newletsassemble.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Table(
        name = "party_chat_meta"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyChatMeta {

    @Id
    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "last_seq", nullable = false)
    private long lastSeq;

    @Column(name = "update_at",nullable = false)
    private Instant updateAt;

    private PartyChatMeta(Long partyId,long lastSeq,Instant updateAt){
        this.partyId = partyId;
        this.lastSeq = lastSeq;
        this.updateAt = updateAt;
    }
    public static PartyChatMeta init(Long partyId,Instant now){
        return new PartyChatMeta(partyId,0L,now);
    }
    public void advance(long newSeq,Instant now){
        if(newSeq > lastSeq){
            this.lastSeq = newSeq;
            this.updateAt = now;
        }
    }
}
