package com.pr1.newletsassemble.auth.infra.http.device;


public record DeviceInfo(
        DevicePlatform platform,
        DeviceBrowser browser,
        String displayName,
        String deviceKey,
        String ip,
        String userAgent
) {

}
