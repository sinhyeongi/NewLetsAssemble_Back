package com.pr1.newletsassemble.global.security.jwt;

public record AccessTokenAuth(Long userId, String role, long tokenVersion,String sid,String deviceKey) {

}
