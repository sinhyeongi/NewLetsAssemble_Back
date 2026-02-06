package com.pr1.newletsassemble.party.domain.interest;

import com.pr1.newletsassemble.party.domain.Interest;
import com.pr1.newletsassemble.party.domain.Party;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "party_interests"
)
public class PartyInterest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id",nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    private PartyInterest(Party party, Interest interest){
        this.party = party;
        this.interest = interest;
    }

    public static PartyInterest of(Party party, Interest interest){
        return new PartyInterest(party,interest);
    }
}
