package com.pr1.newletsassemble.auth.infra.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventLogger {
    private static final Logger SEC = LoggerFactory.getLogger("SECURITY_EVENT");

    public void loginSucceeded(long userId,String deviceKey,String sid){
        withUser(userId,()->
            SEC.info("LOGIN_SUCCEEDED devicekey={} sid={}",mask(deviceKey),sid)
        );
    }
    public void deviceKeyMismatch(long userId,String sid,String tokenDeviceKey,String headerDeviceKey){
        withUser(userId,()->
                SEC.warn("DEVICE_KEY_MISMATCH sid={} tokenDeviceKey={} headerDeviceKey={}",
                        sid,mask(tokenDeviceKey),mask(headerDeviceKey))
        );
    }
    public void refreshReuseDetected(long userId,String sid){
        withUser(userId,()->SEC.error("REFRESH_REUSE_DETECTED sid={}",sid));
    }
    public void logoutAll(long userId,String reason){
        withUser(userId,()->SEC.info("LOGOUT_ALL reason={}",reason));
    }
    private static void withUser(long userId, Runnable r){
        String prev = MDC.get("userId");
        try{
            MDC.put("userId",String.valueOf(userId));
            r.run();
        }finally{
            if(prev == null) MDC.remove("userId");
            else MDC.put("userId",prev);
        }
    }
    private static String mask(String v){
        if(v == null || v.isBlank()) return "null";
        if(v.length() <= 8) return "****";
        return v.substring(0,4) + "****" + v.substring(v.length()-4);
    }
}
