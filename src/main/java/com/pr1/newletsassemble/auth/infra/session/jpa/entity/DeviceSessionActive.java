package com.pr1.newletsassemble.auth.infra.session.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor( access = AccessLevel.PROTECTED)
@Table(
        name = "device_session_active",
        uniqueConstraints = {
                @UniqueConstraint( name = "uk_active_user_device", columnNames = {"user_id","device_key"})
        },
        indexes = {
                @Index(name = "idx_active_user" , columnList = "user_id"),
                @Index(name = "idx_active_last_seen", columnList = "last_seen_at")
        }
)
public class DeviceSessionActive {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "user_id",nullable = false)
    private Long userId;

    @Column(name = "device_key",nullable = false,length = 64)
    private String deviceKey;

    @Column(name = "session_id",nullable = false,length = 36)
    private String sessionId;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
    private DeviceSessionActive(Long userId, String deviceKey, String sessionId, Instant now){
        this.userId = userId;
        this.deviceKey = deviceKey;
        this.sessionId = sessionId;
        this.createdAt = now;
        this.lastSeenAt = now;
    }
    public static DeviceSessionActive create(Long userId, String deviceKey, String sessionId, Instant now){
        return new DeviceSessionActive(userId,deviceKey,sessionId,now);
    }

}
