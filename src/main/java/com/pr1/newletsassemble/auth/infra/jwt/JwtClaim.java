package com.pr1.newletsassemble.auth.infra.jwt;

public enum JwtClaim {
    ROLE("role"),
    TYPE("type"),
    VER("ver"),
    SID("sid"),
    DEVICE_KEY("deviceKey");
    private final String key;
    JwtClaim(String key) {
        this.key = key;
    }
    public String key() {
        return key;
    }
}
