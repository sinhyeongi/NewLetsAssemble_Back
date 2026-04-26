package com.pr1.newletsassemble.user.domain;

public enum Role {
    USER, ADMIN;
    public String getAuthority(){
        return "ROLE_" + name();
    }
}
