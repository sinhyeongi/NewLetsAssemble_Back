package com.pr1.newletsassemble.auth.infra.session.jdbc.dao;

import java.time.Instant;

public interface DeviceSessionActiveUpsertDao {
    String upsertAndReturnOldSid(Long userId, String deviceKey, String newSid, Instant now);
}
