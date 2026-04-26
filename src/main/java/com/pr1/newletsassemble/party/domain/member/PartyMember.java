package com.pr1.newletsassemble.party.domain.member;

import com.pr1.newletsassemble.user.domain.User;
import com.pr1.newletsassemble.party.domain.Party;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "party_member",
        uniqueConstraints = @UniqueConstraint(name = "uk_party_member_party_member", columnNames = {"party_id","user_id"}),
        indexes = {
                @Index(name = "idx_party_member_party_id", columnList = "party_id"),
                @Index(name = "idx_party_member_user_id", columnList = "user_id"),
                @Index(name = "idx_party_member_party_status", columnList = "party_id,status")
        }
)
public class PartyMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_member_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id",nullable = false)
    private Party party;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartyMemberStatus status;
    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt; // 신청일
    @Column(name = "is_black",nullable = false)
    private boolean black;
    @Column(name = "nick_name",length = 40)
    private String nickName;
    @Column(name = "last_read_seq",nullable = false,columnDefinition = "default 0")
    private long lastReadSeq;
    @Column(name = "last_read_at")
    private Instant lastReadAt;


    private PartyMember(Party party,User user,PartyMemberStatus status,Instant appliedAt, String nickName){
        this.party = party;
        this.user = user;
        this.status = status;
        this.appliedAt = appliedAt;
        this.black = false;
        this.nickName = nickName;
        this.lastReadSeq = 0L;
    }
    public static PartyMember create(Party partyId,User applicantId, PartyMemberStatus status, Instant applicantDay, String nickName){
        return new PartyMember(partyId,applicantId,status,applicantDay,nickName);
    }
}
