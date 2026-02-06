package com.pr1.newletsassemble.auth.application;

import com.pr1.newletsassemble.auth.infra.jwt.JwtProperties;
import com.pr1.newletsassemble.auth.infra.redis.key.RedisKeys;
import com.pr1.newletsassemble.global.time.TimeProvider;
import com.pr1.newletsassemble.auth.infra.logging.SecurityEventLogger;
import com.pr1.newletsassemble.auth.infra.redis.repository.DeviceKeyMismatchCounterRepository;
import com.pr1.newletsassemble.auth.infra.redis.repository.RefreshTokenRepository;
import com.pr1.newletsassemble.auth.infra.redis.repository.TokenVersionRepository;
import com.pr1.newletsassemble.auth.infra.session.jpa.repository.DeviceSessionActiveJpaRepository;
import com.pr1.newletsassemble.auth.infra.session.jpa.repository.DeviceSessionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AccountProtectionService {
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final long SID_THRESHOLD = 3;  // 해당 기기만 로그아웃
    private static final long USER_THRESHOLD = 10; // 전체 기기 로그아웃

    private final DeviceKeyMismatchCounterRepository counter;
    private final DeviceSessionJpaRepository deviceSessionJpaRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenVersionRepository tokenVersionRepository;
    private final DeviceSessionActiveJpaRepository deviceSessionActiveJpaRepository;
    private final JwtProperties jwtProperties;

    private final TimeProvider timeProvider;
    private final SecurityEventLogger log;
    public enum ProtectionAction{
        NONE,
        LOGOUT_SESSION,
        LOGOUT_ALL
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProtectionAction onDeviceKeyMismatch(long userId,String sid,
                                       String expectedDeviceKey, String presentedDeviceKey,
                                       String ip, String ua){
        long sidCounter = counter.incrSid(RedisKeys.deviceKeyMismatchCounterSid(userId,sid),WINDOW);
        long userCounter = counter.incrUser(RedisKeys.deviceKeyMismatchCounterUser(userId),WINDOW);

        log.deviceKeyMismatch(userId,sid,expectedDeviceKey,presentedDeviceKey);
        // 10회(5분) -> 전체 로그아웃 ( 계정 보호 )
        if(userCounter >= USER_THRESHOLD ){
            log.logoutAll(userId,"deviceKey mismatch >=" + USER_THRESHOLD + " within 5m");
            doLogoutAll(userId);
            return ProtectionAction.LOGOUT_ALL;
        }
        // 3회 (5분 , 같은 sid) -> 해당 기기만 로그아웃
        if(sidCounter >= SID_THRESHOLD){
            // log
            doLogoutSession(userId,sid);
            return ProtectionAction.LOGOUT_SESSION;
        }
        return ProtectionAction.NONE;
    }
    private void doLogoutSession(long userId,String sid){
        deviceSessionJpaRepository.findByUserIdAndSessionId(userId,sid)
                .ifPresent(s -> s.revoke(timeProvider.now()));
        refreshTokenRepository.deleteOneAtomic(userId,sid);
        counter.resetSid(RedisKeys.deviceKeyMismatchCounterSid(userId,sid));
    }
    private void doLogoutAll(long userId){
        tokenVersionRepository.bump(userId,Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()).plusDays(3));
        deviceSessionJpaRepository.revokeAllByUserId(userId,timeProvider.now());
        deviceSessionActiveJpaRepository.deleteByUserId(userId);
        refreshTokenRepository.deleteAllAtomic(userId);

        counter.resetUser(RedisKeys.deviceKeyMismatchCounterUser(userId));
    }
}
