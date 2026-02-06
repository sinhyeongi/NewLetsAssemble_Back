package com.pr1.newletsassemble.auth.infra.jwt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Min(60_000) long accessTokenExpirationMs,
        @Min(60_000) long refreshTokenExpirationMs,
        @NotBlank String refreshHashPepper
        ) {}
