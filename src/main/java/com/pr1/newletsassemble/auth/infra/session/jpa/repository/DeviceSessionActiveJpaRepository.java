package com.pr1.newletsassemble.auth.infra.session.jpa.repository;

import com.pr1.newletsassemble.auth.infra.session.jpa.entity.DeviceSessionActive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceSessionActiveJpaRepository extends JpaRepository<DeviceSessionActive, Long> {

    @Query("""
    select d.sessionId
    from DeviceSessionActive as d
    where d.userId = :userId
    and d.deviceKey = :deviceKey
""")
    Optional<String> findSessionIdByUserIdAndDeviceKey(long userId, String deviceKey);
    void deleteByUserIdAndDeviceKey(long userId, String deviceKey);
    void deleteByUserId(Long userId);
}
