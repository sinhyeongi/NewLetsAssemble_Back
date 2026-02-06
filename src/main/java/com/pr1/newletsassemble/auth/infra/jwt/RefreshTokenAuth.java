package com.pr1.newletsassemble.auth.infra.jwt;

public record RefreshTokenAuth(Long userId,String sessionId,long tokenVersion,String deviceKey) {
}
