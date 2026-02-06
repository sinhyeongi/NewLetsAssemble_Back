package com.pr1.newletsassemble.auth.api.dto;

public record LoginTokens(
    String accessToken,
    String refreshToken,
    String deviceKey
) {}
