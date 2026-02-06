package com.pr1.newletsassemble.auth.application;

import com.pr1.newletsassemble.auth.api.dto.LoginRequest;
import com.pr1.newletsassemble.auth.api.dto.LoginTokens;
import com.pr1.newletsassemble.auth.api.dto.LogoutRequest;
import com.pr1.newletsassemble.auth.api.dto.ReissueTokens;
import com.pr1.newletsassemble.auth.infra.jwt.JwtProperties;
import com.pr1.newletsassemble.auth.infra.jwt.JwtProvider;
import com.pr1.newletsassemble.auth.infra.jwt.RefreshTokenAuth;
import com.pr1.newletsassemble.auth.infra.jwt.RefreshTokenHasher;
import com.pr1.newletsassemble.auth.infra.security.DeviceKeyGenerator;
import com.pr1.newletsassemble.auth.infra.session.*;
import com.pr1.newletsassemble.auth.infra.session.jdbc.dao.DeviceSessionActiveUpsertDao;
import com.pr1.newletsassemble.auth.infra.session.jpa.entity.DeviceSession;
import com.pr1.newletsassemble.auth.infra.session.jpa.repository.DeviceSessionActiveJpaRepository;
import com.pr1.newletsassemble.auth.infra.session.jpa.repository.DeviceSessionJpaRepository;
import com.pr1.newletsassemble.user.domain.Role;
import com.pr1.newletsassemble.user.domain.User;
import com.pr1.newletsassemble.global.error.ApiException;
import com.pr1.newletsassemble.global.error.AuthErrorCode;
import com.pr1.newletsassemble.global.time.TimeProvider;
import com.pr1.newletsassemble.auth.infra.http.device.DeviceInfo;
import com.pr1.newletsassemble.auth.infra.logging.SecurityEventLogger;
import com.pr1.newletsassemble.user.infra.persistence.jpa.UserJpaRepository;
import com.pr1.newletsassemble.auth.infra.redis.repository.DeviceKeyMismatchCounterRepository;
import com.pr1.newletsassemble.auth.infra.redis.repository.LoginReplayCacheRepository;
import com.pr1.newletsassemble.auth.infra.redis.repository.RefreshTokenRepository;
import com.pr1.newletsassemble.auth.infra.redis.repository.TokenVersionRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceKeyGenerator deviceKeyGenerator;

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenVersionRepository tokenVersionRepository;
    private final DeviceKeyMismatchCounterRepository counter;

    private final DeviceSessionJpaRepository deviceSessionJpaRepository;
    private final DeviceSessionActiveUpsertDao deviceSessionActiveDao;
    private final DeviceSessionActiveJpaRepository deviceSessionActiveJpaRepository;
    private final LoginReplayCacheRepository loginReplayCacheRepository;

    private final AccountProtectionService protectionService;
    private final TimeProvider timeProvider;
    private final SecurityEventLogger log;
    /**
     *  Login
     * @param request
     * @param deviceInfo
     * @return
     */
    @Transactional
    public LoginTokens login(LoginRequest request, DeviceInfo deviceInfo){

        User user = userJpaRepository.findByEmail(request.email()).orElseThrow(()-> ApiException.of(AuthErrorCode.AUTH_ACCOUNT_NOT_FOUND));
        if(!passwordEncoder.matches(request.password(),user.getPassword())){
            throw ApiException.of(AuthErrorCode.AUTH_BAD_CREDENTIALS);
        }
        long userId = user.getId();
        String incomingDeviceKey = normalizeDeviceKey(deviceInfo.deviceKey());

        String idemKey = buildIdemKey(userId,incomingDeviceKey,deviceInfo);
        LoginTokens cached = loginReplayCacheRepository.get(idemKey);
        if(cached != null){
            return cached;
        }

        final Duration LOCK_TTL = Duration.ofSeconds(4);
        final Duration CACHE_TTL = Duration.ofSeconds(3);

        String ownerToken = UUID.randomUUID().toString();
        LoginReplayCacheRepository.LockHandle lock;
        try{
            lock = loginReplayCacheRepository.tryLock(idemKey,ownerToken,LOCK_TTL);
        }catch(Exception e){
            throw ApiException.of(AuthErrorCode.AUTH_LOGIN_CONFLICT);
        }
        if(lock == null){
            LoginTokens waited = waitForLoginCache(idemKey,Duration.ofMillis(400));
            if(waited != null){
                return waited;
            }
            throw ApiException.of(AuthErrorCode.AUTH_LOGIN_CONFLICT);
        }
        try{
            cached = loginReplayCacheRepository.get(idemKey);
            if(cached != null){
                return cached;
            }

            //처리 중 TTL 연장 (대형 처리 대비)
            renewOr409(lock,LOCK_TTL);
            String deviceKey = incomingDeviceKey;
            if(deviceKey == null){
                deviceKey = deviceKeyGenerator.newKey();
            }

            long version = tokenVersionRepository.getOrInit(userId,Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()).plusDays(3));
            String sid = createUniqueSidWithInsert(userId,deviceInfo,deviceKey);
            String oldSid = deviceSessionActiveDao.upsertAndReturnOldSid(userId,deviceKey,sid,timeProvider.now());

            renewOr409(lock,LOCK_TTL);

            if(oldSid != null && !oldSid.isBlank() && !oldSid.equals(sid)){
                revokedSessionAndRefresh(userId,oldSid,timeProvider.now());
            }

            String role = user.getRole().getAuthority();

            String access = jwtProvider.createAccessToken(userId,role,sid,version,deviceKey);
            String refresh = jwtProvider.createRefreshToken(userId,sid,version,deviceKey);

            String hash = RefreshTokenHasher.sha256Hex(refresh + jwtProperties.refreshHashPepper());
            Duration ttl = Duration.ofMillis(jwtProperties.refreshTokenExpirationMs());

            boolean saved = refreshTokenRepository.saveHashAtomic(userId,sid,hash,ttl);
            if(!saved){
                throw new IllegalStateException("SAVE_FAILED_REFRESH_HASH userId = %d , sid = %s".formatted(userId,sid));
            }
            cached = new LoginTokens(access,refresh,deviceKey);
            try{
                loginReplayCacheRepository.put(idemKey,cached,CACHE_TTL);
            }catch(Exception e){
                throw ApiException.of(AuthErrorCode.AUTH_LOGIN_CONFLICT);
            }
            user.recordLogin(timeProvider.now());
            return cached;
        }finally{
            try{
                loginReplayCacheRepository.unlock(lock);
            }catch(Exception ignored){

            }
        }
    }
    /**
     * Reissue
     */
    @Transactional
    public ReissueTokens reissue(String refreshCookieValue, DeviceInfo deviceInfo, HttpServletResponse res){
        if(refreshCookieValue == null || refreshCookieValue.isBlank()){
            throw ApiException.of(AuthErrorCode.AUTH_REFRESH_EXPIRED);
        }
        RefreshTokenAuth auth = jwtProvider.authenticateRefresh(refreshCookieValue);
        String deviceKey = normalizeDeviceKey(deviceInfo.deviceKey());

        if(deviceKey == null || deviceKey.isBlank()){
            throw ApiException.of(AuthErrorCode.AUTH_DEVICE_KEY_MISMATCH);
        }
        if(auth.deviceKey() == null){
            throw ApiException.of(AuthErrorCode.AUTH_DEVICE_KEY_MISSING);
        }
        if(!auth.deviceKey().equals(deviceKey)){
           AccountProtectionService.ProtectionAction action =
            protectionService.onDeviceKeyMismatch(
                   auth.userId(),
                   auth.sessionId(),
                   auth.deviceKey(),
                   deviceKey,
                   deviceInfo.ip(),
                   deviceInfo.userAgent()
                   );
           if(action == AccountProtectionService.ProtectionAction.LOGOUT_ALL){
               throw ApiException.of(AuthErrorCode.AUTH_DEVICE_KEY_MISMATCH_LOGOUT_ALL);
           }
           throw ApiException.of(AuthErrorCode.AUTH_DEVICE_KEY_MISMATCH);

        }
        long userId = auth.userId();
        long serverVersion = tokenVersionRepository.getOrInit(userId,Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()).plusDays(3));

        if(auth.tokenVersion() != serverVersion){
            throw new ApiException(AuthErrorCode.AUTH_REFRESH_REUSE_DETECTED);
        }

        String sid = auth.sessionId();
        DeviceSession session = deviceSessionJpaRepository.findByUserIdAndSessionId(userId,sid)
                .orElseThrow(() -> new ApiException(AuthErrorCode.AUTH_DEVICE_SESSION_NOT_FOUND));

        if(session.isRevoked()){
            throw ApiException.of(AuthErrorCode.AUTH_DEVICE_SESSION_REVOKED);
        }
        if(!session.getDeviceKey().equals(deviceKey)){
            throw ApiException.of(AuthErrorCode.AUTH_DEVICE_KEY_MISMATCH);
        }

        // refresh rotate
        String newRefreshToken = jwtProvider.createRefreshToken(userId,sid,serverVersion,deviceKey);

        String oldHash = RefreshTokenHasher.sha256Hex(refreshCookieValue + jwtProperties.refreshHashPepper());
        String newHash = RefreshTokenHasher.sha256Hex(newRefreshToken + jwtProperties.refreshHashPepper());
        Duration ttl = Duration.ofMillis(jwtProperties.refreshTokenExpirationMs());

        RefreshTokenRotateResult result = refreshTokenRepository.rotate(userId,sid,oldHash,newHash,ttl);

        if(result == RefreshTokenRotateResult.REUSED){
            tokenVersionRepository.bump(userId,Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()).plusDays(3));
            session.revoke(timeProvider.now());
            refreshTokenRepository.deleteAllAtomic(userId);
            throw new ApiException(AuthErrorCode.AUTH_REFRESH_REUSE_DETECTED);
        }
        if(result == RefreshTokenRotateResult.NOT_FOUND){
            throw new ApiException(AuthErrorCode.AUTH_REFRESH_EXPIRED);
        }

        Role role = userJpaRepository.findById(userId)
                .orElseThrow(()-> new ApiException((AuthErrorCode.AUTH_ACCOUNT_NOT_FOUND))).getRole();

        String access = jwtProvider.createAccessToken(userId,role.getAuthority(),sid,serverVersion,deviceKey);
        session.touch(timeProvider.now());
        counter.resetSid(userId,sid);
        counter.resetUser(userId);
        return new ReissueTokens(access,newRefreshToken);
    }
    /**
     *  Logout
     */
    @Transactional
    public void logOut(LogoutRequest request){
        RefreshTokenAuth token;
        try {
         token = jwtProvider.authenticateRefresh(request.refreshToken());
        }catch(JwtException | IllegalArgumentException e){
            return;
        }
        deviceSessionJpaRepository.findByUserIdAndSessionId(token.userId(),token.sessionId())
                        .ifPresent(session -> session.revoke(timeProvider.now()));
        refreshTokenRepository.deleteOneAtomic(token.userId(), token.sessionId());
    }

    /**
     * 전체 기기 로그아웃 ( 로그인 한 사용자 )
     * @param userId
     */
    @Transactional
    public void logOutALL(long userId){
        tokenVersionRepository.bump(userId,Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()).plusDays(3));
        deviceSessionJpaRepository.revokeAllByUserId(userId,timeProvider.now());
        deviceSessionActiveJpaRepository.deleteByUserId(userId);
        refreshTokenRepository.deleteAllAtomic(userId);
    }

    private void revokedSessionAndRefresh(long userId, String sid, Instant now){
        deviceSessionJpaRepository.findByUserIdAndSessionId(userId,sid)
                .ifPresent(s -> s.revoke(now));
        refreshTokenRepository.deleteOneAtomic(userId,sid);
    }



    /**
     * random UUID try 3 -> throw IllegalStateException("SID_GENERATION_FAILED")
     *
     * @return random UUID
     */
    private String createUniqueSidWithInsert(Long userId, DeviceInfo deviceInfo,String deviceKey){
        for(int i = 0 ; i < 3; i++){
            String sid = UUID.randomUUID().toString();
            try{
                deviceSessionJpaRepository.save(
                        DeviceSession.create(userId,sid,deviceKey
                        ,deviceInfo.displayName(),timeProvider.now())
                );

                return sid;
            }catch(DataIntegrityViolationException e){

            }
        }
        throw new IllegalStateException("SID_GENERATION_FAILED");
    }

    private void renewOr409(LoginReplayCacheRepository.LockHandle lock, Duration ttl){
        boolean ok;
        try{
            ok = loginReplayCacheRepository.renew(lock, ttl);
        }catch(Exception e){
            throw ApiException.of(AuthErrorCode.AUTH_LOGIN_CONFLICT);
        }
        if(!ok){
            throw ApiException.of(AuthErrorCode.AUTH_LOGIN_CONFLICT);
        }
    }
    /**
     *
     * @param idemKey
     * @param maxWait ->
     * @return
     */
    private LoginTokens waitForLoginCache(String idemKey, Duration maxWait){
        long deadline = System.nanoTime() + maxWait.toNanos();
        while(System.nanoTime() < deadline){
            LoginTokens cache = loginReplayCacheRepository.get(idemKey);
            if(cache != null) return cache;
            try{
                Thread.sleep(50);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private String buildIdemKey(long userId, String deviceKey,DeviceInfo deviceInfo){
        if(deviceKey != null){
            return userId + ":" + deviceKey;
        }
        String fp = bootstrapFingerprint(deviceInfo);
        return userId + ":bootstrap:" + fp;
    }
    private String bootstrapFingerprint(DeviceInfo deviceInfo){
        String ip = deviceInfo.ip();
        String ua = deviceInfo.userAgent();
        String raw = (ip == null ? "" : ip) + "|" + (ua == null ? "" : ua);
        return RefreshTokenHasher.sha256Hex(raw).substring(0,16);
    }
    private String normalizeDeviceKey(String deviceKey){
        if(deviceKey == null){return null;}
        String v = deviceKey.trim();
        return v.isEmpty() ? null : v;
    }


}
