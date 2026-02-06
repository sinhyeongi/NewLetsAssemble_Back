package com.pr1.newletsassemble.chat.domain;

import com.pr1.newletsassemble.party.domain.Party;
import com.pr1.newletsassemble.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "chats",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_chat_client_msg",
                    columnNames = {"party_id","sender_id","client_message_id"}
            )
        },
        indexes = {
                @Index(name = "idx_chat_party_id_id", columnList = "party_id, id"),
                @Index(name = "idx_chat_party_sender_id_id", columnList = "party_id, sender_id, id"),
                @Index(name = "idx_chat_sender_created_at", columnList = "sender_id, created_at"),
                @Index(name = "idx_chat_created_at", columnList = "created_at")
        }
)
public class Chat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "party_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Party party;
    @Column(name = "seq", nullable = false)
    private long seq;
    @JoinColumn(name = "sender_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User sender;
    @Column(name = "client_message_id", length = 36, nullable = false)
    private String clientMessageId;// 모바일 재전송

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatType type;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false,updatable = false)
    private Instant createdAt;

    private Chat(Party party, long seq,User sender, String clientMessageId ,ChatType type, String content){
        this.party = party;
        this.seq = seq;
        this.sender = sender;
        this.clientMessageId = clientMessageId;
        this.type = type;
        this.content = content;

    }
    public static Chat create(Party party,long seq,User sender, String clientMessageId ,ChatType type, String content){
        return new Chat(party, seq,sender, clientMessageId,type,content);
    }

    public void softDeleted(Instant now){
        this.deletedAt = now;
    }


}
