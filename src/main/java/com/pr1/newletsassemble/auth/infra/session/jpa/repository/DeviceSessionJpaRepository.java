package com.pr1.newletsassemble.auth.infra.session.jpa.repository;

import com.pr1.newletsassemble.auth.infra.session.jpa.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface DeviceSessionJpaRepository extends JpaRepository<DeviceSession,Long> {
    Optional<DeviceSession> findBySessionId(String sessionId);
    Optional<DeviceSession> findByUserIdAndSessionId(Long userId,String sessionId);

    Optional<DeviceSession> findByUserIdAndDeviceKeyAndRevokedFalse(Long userId,String deviceKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE DeviceSession d
                        set d.revoked = true,
                        d.revokedAt = :now
                            where d.userId = :userId
                                and d.revoked = false                        
    """
    )
    int revokeAllByUserId(@Param("userId") Long userId,@Param("now") Instant now);

    void deleteBySessionId(String sessionId);
    void deleteById(Long id);
    void deleteByUserIdAndSessionId(Long userId,String sessionId);

}
