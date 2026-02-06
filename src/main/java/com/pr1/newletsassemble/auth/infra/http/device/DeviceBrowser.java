package com.pr1.newletsassemble.auth.infra.http.device;

public enum DeviceBrowser {
    CHROME("Chrome"),
    FIREFOX("Firefox"),
    SAFARI("Safari"),
    EDGE("Edge"),
    OPERA("Opera"),
    UNKNOWN("Unknown");

    private final String displayName;
    DeviceBrowser(String displayName){
        this.displayName = displayName;
    }
    public String displayName(){
        return displayName;
    }
}
