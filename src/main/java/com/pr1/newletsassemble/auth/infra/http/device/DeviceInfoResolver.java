package com.pr1.newletsassemble.auth.infra.http.device;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeviceInfoResolver {

    public DeviceInfo resolve(HttpServletRequest req){
        String ua = req.getHeader("User-Agent");
        String xdk = resolveDeviceKey(req);

        DevicePlatform platform = resolvePlatform(ua);
        DeviceBrowser browser = resolveBrowser(ua);
        String displayName = buildDisplayName(platform,browser);
        String xff = resolveClientIp(req);
        return new DeviceInfo(platform,browser,displayName,xdk,xff,ua);
    }
    /* ================= Platform ================= */

    private DevicePlatform resolvePlatform(String ua){
        if(ua == null){return DevicePlatform.UNKNOWN;}
        ua = ua.toLowerCase();
        if(ua.contains("android")){return DevicePlatform.ANDROID;}
        if(ua.contains("iphone") || ua.contains("ipad")){return DevicePlatform.IOS;}
        if(ua.contains("windows")){return DevicePlatform.WEB_WINDOW;}
        if(ua.contains("mac os")){return DevicePlatform.WEB_MAC;}
        if(ua.contains("linux")){return DevicePlatform.WEB_LINUX;}
        return DevicePlatform.UNKNOWN;
    }

    /* ================= Browser ================= */
    private DeviceBrowser resolveBrowser(String ua){
        if(ua == null){return DeviceBrowser.UNKNOWN;}
        ua = ua.toLowerCase();
        if(ua.contains("edg")){return DeviceBrowser.EDGE;}
        if(ua.contains("opr")){return DeviceBrowser.OPERA;}
        if(ua.contains("chrome")){return DeviceBrowser.CHROME;}
        if(ua.contains("firefox")){return DeviceBrowser.FIREFOX;}
        if(ua.contains("safari")){return DeviceBrowser.SAFARI;}
        return DeviceBrowser.UNKNOWN;
    }
    private String buildDisplayName(DevicePlatform platform, DeviceBrowser browser){
        return platform.displayName() + " · " + browser.displayName();
    }


    public boolean validUUID(String uuid){
        if(uuid == null || uuid.isBlank()){return false;}
        try{
            UUID.fromString(uuid);
            return true;
        }catch(IllegalArgumentException e){
            return false;
        }
    }
    private String resolveClientIp(HttpServletRequest req){
        String xff = req.getHeader("X-Forwarded-For");
        if(xff != null && !xff.isBlank()){
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
    private String resolveDeviceKey(HttpServletRequest req){
        if(req.getCookies() == null){return null;}
        for(Cookie c : req.getCookies()){
            if("X-LA-Device-Id".equals(c.getName())){
                return c.getValue();
            }
        }
        return null;
    }
}
