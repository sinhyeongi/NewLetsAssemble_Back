package com.pr1.newletsassemble.global.security.jwt;

public record RefreshTokenAuth(Long userId,String sessionId,long tokenVersion,String deviceKey) {
}
