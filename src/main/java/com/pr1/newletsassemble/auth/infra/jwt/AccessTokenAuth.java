package com.pr1.newletsassemble.auth.infra.jwt;

public record AccessTokenAuth(Long userId, String role, long tokenVersion,String sid,String deviceKey) {

}
