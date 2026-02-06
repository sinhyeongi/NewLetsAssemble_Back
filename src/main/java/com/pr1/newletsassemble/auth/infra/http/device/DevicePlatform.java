package com.pr1.newletsassemble.auth.infra.http.device;

public enum DevicePlatform {
    WEB_WINDOW("Windows"),
    WEB_MAC("Mac"),
    WEB_LINUX("Linux"),
    ANDROID("Android"),
    IOS("ios"),
    UNKNOWN("Unknown");

    private final String displayName;

    DevicePlatform(String displayName){
        this.displayName = displayName;
    }
    public String displayName(){
        return displayName;
    }

}
