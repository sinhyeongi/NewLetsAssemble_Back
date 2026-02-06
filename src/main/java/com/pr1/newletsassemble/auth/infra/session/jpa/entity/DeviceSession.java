package com.pr1.newletsassemble.auth.infra.session.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table( name = "device_session",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_id",
                        columnNames = {"session_id"}
                )
        },
        indexes ={
        @Index(name = "idx_device_user", columnList = "user_id"),
        @Index(name = "idx_device_user_device_active", columnList="user_id, device_key, revoked"),
        @Index(name = "idx_device_last_seen", columnList = "last_seen_at")
})
@NoArgsConstructor
public class DeviceSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    @Column(name = "session_id", nullable = false, length = 36, updatable = false)
    private String sessionId;
    private String deviceKey;
    private String displayName;

    private Instant createAt;
    private Instant lastSeenAt;

    private boolean revoked;
    private Instant revokedAt;
    private DeviceSession(Long userId, String sessionId, String deviceKey,String displayName, Instant now){
        this.userId = userId;
        this.sessionId = sessionId;
        this.deviceKey = deviceKey;
        this.displayName = displayName;
        this.createAt = now;
        this.lastSeenAt = now;
        this.revoked = false;
    }
    public static DeviceSession create(Long userId, String sessionId,String deviceKey, String displayName, Instant now){
        return new DeviceSession(userId,sessionId,deviceKey,displayName,now);
    }
    public void touch(Instant now){
        this.lastSeenAt = now;
    }
    public void revoke(Instant now){
        this.revoked = true;
        revokedAt = now;
    }
}
