package com.pr1.newletsassemble.auth.infra.session.jdbc.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
@Repository
@RequiredArgsConstructor
public class DeviceSessionActiveUpsertDaoImpl implements DeviceSessionActiveUpsertDao {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String upsertAndReturnOldSid(Long userId, String deviceKey, String newSid, Instant now) {
        String oldSid = jdbcTemplate.query(
                "SELECT session_id FROM device_session_active WHERE user_id = ? AND device_key = ?" ,
                ps -> {
                    ps.setLong(1,userId);
                    ps.setString(2,deviceKey);
                },
                rs ->
                        rs.next() ? rs.getString(1) : null
        );
        jdbcTemplate.update(
                """
                        INSERT INTO device_session_active (user_id, device_key, session_id, created_at, last_seen_at)
                        VALUES(?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            session_id = VALUES(session_id),
                            last_seen_at = VALUES(last_seen_at)
                        """,
                userId,deviceKey,newSid,now,now
        );
        return oldSid;
    }
}
