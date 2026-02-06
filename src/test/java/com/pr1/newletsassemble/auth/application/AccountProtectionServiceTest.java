package com.pr1.newletsassemble.auth.application;

import com.pr1.newletsassemble.auth.infra.jwt.JwtProperties;
import com.pr1.newletsassemble.auth.infra.logging.SecurityEventLogger;
import com.pr1.newletsassemble.auth.infra.redis.key.RedisKeys;
import com.pr1.newletsassemble.auth.infra.redis.repository.DeviceKeyMismatchCounterRepository;
import com.pr1.newletsassemble.auth.infra.redis.repository.RefreshTokenRepository;
import com.pr1.newletsassemble.auth.infra.redis.repository.TokenVersionRepository;
import com.pr1.newletsassemble.auth.infra.session.jpa.repository.DeviceSessionActiveJpaRepository;
import com.pr1.newletsassemble.auth.infra.session.jpa.repository.DeviceSessionJpaRepository;
import com.pr1.newletsassemble.global.time.TimeProvider;
import com.pr1.newletsassemble.auth.infra.session.jpa.entity.DeviceSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountProtectionServiceTest {

    @Mock DeviceKeyMismatchCounterRepository counter;
    @Mock
    DeviceSessionJpaRepository deviceSessionJpaRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock TokenVersionRepository tokenVersionRepository;
    @Mock
    DeviceSessionActiveJpaRepository deviceSessionActiveJpaRepository;
    @Mock TimeProvider timeProvider;
    @Mock SecurityEventLogger log;
    @Mock JwtProperties jwtProperties;
    AccountProtectionService service;

    @BeforeEach
    void setUp() {
        service = new AccountProtectionService(
                counter,
                deviceSessionJpaRepository,
                refreshTokenRepository,
                tokenVersionRepository,
                deviceSessionActiveJpaRepository,
                jwtProperties,
                timeProvider,
                log

        );
    }

    @Test
    void mismatch_under_threshold_should_return_NONE_and_do_nothing() {
        long userId = 1L;
        String sid = "sid-1";

        when(counter.incrSid(eq(RedisKeys.deviceKeyMismatchCounterSid(userId,sid)), any(Duration.class))).thenReturn(2L);
        when(counter.incrUser(eq(RedisKeys.deviceKeyMismatchCounterUser(userId)), any(Duration.class))).thenReturn(2L);

        AccountProtectionService.ProtectionAction action = service.onDeviceKeyMismatch(
                userId, sid,
                "expected", "presented",
                "1.1.1.1", "ua"
        );

        assertThat(action).isEqualTo(AccountProtectionService.ProtectionAction.NONE);

        // 로그는 남기되
        verify(log).deviceKeyMismatch(eq(userId), eq(sid), anyString(), anyString());

        // 세션 revoke / 토큰 삭제는 없어야 함
        verify(deviceSessionJpaRepository, never()).findByUserIdAndSessionId(anyLong(), anyString());
        verify(deviceSessionJpaRepository, never()).revokeAllByUserId(anyLong(), any());
        verify(refreshTokenRepository, never()).deleteOneAtomic(anyLong(), anyString());
        verify(refreshTokenRepository, never()).deleteAllAtomic(anyLong());
        verify(tokenVersionRepository, never()).bump(anyLong(),Duration.ofDays(33));
        verify(counter, never()).resetSid(anyString());
        verify(counter, never()).resetUser(anyString());
    }

    @Test
    void mismatch_3_times_same_sid_should_logout_session_only() {
        long userId = 1L;
        String sid = "sid-1";
        Instant now = Instant.parse("2026-01-28T12:00:00Z");

        when(counter.incrSid(eq(RedisKeys.deviceKeyMismatchCounterSid(userId,sid)), any(Duration.class))).thenReturn(3L);
        when(counter.incrUser(eq(RedisKeys.deviceKeyMismatchCounterUser(userId)), any(Duration.class))).thenReturn(3L);
        when(timeProvider.now()).thenReturn(now);

        DeviceSession session = mock(DeviceSession.class);
        when(deviceSessionJpaRepository.findByUserIdAndSessionId(userId, sid)).thenReturn(Optional.of(session));

        AccountProtectionService.ProtectionAction action = service.onDeviceKeyMismatch(
                userId, sid,
                "expected", "presented",
                "1.1.1.1", "ua"
        );

        assertThat(action).isEqualTo(AccountProtectionService.ProtectionAction.LOGOUT_SESSION);

        verify(session).revoke(now);
        verify(refreshTokenRepository).deleteOneAtomic(userId, sid);
        verify(counter).resetSid(RedisKeys.deviceKeyMismatchCounterSid(userId,sid));

        // 전체 로그아웃은 아니어야 함
        verify(tokenVersionRepository, never()).bump(anyLong(),Duration.ofDays(33));
        verify(deviceSessionJpaRepository, never()).revokeAllByUserId(anyLong(), any());
        verify(deviceSessionActiveJpaRepository, never()).deleteByUserId(anyLong());
        verify(refreshTokenRepository, never()).deleteAllAtomic(anyLong());
        verify(counter, never()).resetUser(RedisKeys.deviceKeyMismatchCounterUser(userId));
    }

    @Test
    void mismatch_10_times_should_logout_all_devices() {
        long userId = 1L;
        String sid = "sid-1";
        Instant now = Instant.parse("2026-01-28T12:00:00Z");

        // sid는 낮아도, userCounter가 10이면 전체 로그아웃
        when(counter.incrSid(eq(RedisKeys.deviceKeyMismatchCounterSid(userId,sid)), any(Duration.class))).thenReturn(1L);
        when(counter.incrUser(eq(RedisKeys.deviceKeyMismatchCounterUser(userId)), any(Duration.class))).thenReturn(10L);
        when(timeProvider.now()).thenReturn(now);

        AccountProtectionService.ProtectionAction action = service.onDeviceKeyMismatch(
                userId, sid,
                "expected", "presented",
                "1.1.1.1", "ua"
        );

        assertThat(action).isEqualTo(AccountProtectionService.ProtectionAction.LOGOUT_ALL);

        verify(tokenVersionRepository).bump(userId,Duration.ofDays(33));
        verify(deviceSessionJpaRepository).revokeAllByUserId(userId, now);
        verify(deviceSessionActiveJpaRepository).deleteByUserId(userId);
        verify(refreshTokenRepository).deleteAllAtomic(userId);
        verify(counter).resetUser(RedisKeys.deviceKeyMismatchCounterUser(userId));

        // 세션 단건 로그아웃은 하지 않아야 함(전체가 우선)
        verify(deviceSessionJpaRepository, never()).findByUserIdAndSessionId(anyLong(), anyString());
        verify(refreshTokenRepository, never()).deleteOneAtomic(anyLong(), anyString());
        verify(counter, never()).resetSid(anyString());
    }

    @Test
    void when_both_thresholds_reached_should_prefer_logout_all() {
        long userId = 1L;
        String sid = "sid-1";
        Instant now = Instant.parse("2026-01-28T12:00:00Z");

        when(counter.incrSid(eq(RedisKeys.deviceKeyMismatchCounterSid(userId,sid)), any(Duration.class))).thenReturn(3L);
        when(counter.incrUser(eq(RedisKeys.deviceKeyMismatchCounterUser(userId)), any(Duration.class))).thenReturn(10L);
        when(timeProvider.now()).thenReturn(now);

        AccountProtectionService.ProtectionAction action = service.onDeviceKeyMismatch(
                userId, sid,
                "expected", "presented",
                "1.1.1.1", "ua"
        );

        assertThat(action).isEqualTo(AccountProtectionService.ProtectionAction.LOGOUT_ALL);

        verify(tokenVersionRepository).bump(userId,Duration.ofDays(33));
        verify(deviceSessionJpaRepository).revokeAllByUserId(userId, now);
        verify(refreshTokenRepository).deleteAllAtomic(userId);

        // LOGOUT_SESSION 흐름은 타면 안 됨
        verify(deviceSessionJpaRepository, never()).findByUserIdAndSessionId(anyLong(), anyString());
        verify(refreshTokenRepository, never()).deleteOneAtomic(anyLong(), anyString());
        verify(counter, never()).resetSid(anyString());
    }
}