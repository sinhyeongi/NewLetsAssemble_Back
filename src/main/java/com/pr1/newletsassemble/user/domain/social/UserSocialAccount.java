package com.pr1.newletsassemble.user.domain.social;

import com.pr1.newletsassemble.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_social_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_provider_provider_id", columnNames = {"provider","provider_id"})
        },
        indexes  = {
                @Index(name = "idx_user_social_user_id", columnList = "user_id")
        }
)
public class UserSocialAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_social_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_id", nullable = false,length = 100)
    private String providerId;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    private UserSocialAccount(User user,Provider provider, String providerId, Instant linkedAt){
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
        this.linkedAt = linkedAt;
    }
    public static UserSocialAccount of(User user, Provider provider, String providerId, Instant linkedAt){
        return new UserSocialAccount(user,provider,providerId,linkedAt);
    }
}
