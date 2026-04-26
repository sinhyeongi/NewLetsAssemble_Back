package com.pr1.newletsassemble.auth.infra.redis.key;

public final class RedisKeys {
    private RedisKeys(){}

    public static String sessionActive(long userId,String deviceKey){
        return "auth:active:" + userId + ":" + deviceKey;
    }
    public static String deviceKeyMismatchCounterUser(long userId){
        return "sec:dkmm:u:" + userId;
    }
    public static String deviceKeyMismatchCounterSid(long userId,String sid){
        return "sec:dkmm:u:" + userId + ":sid:" + sid;
    }
    public static String loginReplayCache(String idemKey){
        return "login:replay:" + idemKey;
    }
    public static String loginReplayLock(String idemKey){
        return "login:lock:" + idemKey;
    }
    public static String refreshToken(Long userId,String sid){
        return "refresh:" + userId + ":" + sid;
    }
    public static String refreshTokenIndex(Long userId){
        return "refresh:idx:" + userId;
    }
}
