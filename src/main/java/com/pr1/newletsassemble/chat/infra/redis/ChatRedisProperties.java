package com.pr1.newletsassemble.chat.infra.redis;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "chat.redis")
public record ChatRedisProperties(
        @Min(1000) long partyCacheMax,
        @Min(86400000) long partyCacheExpirationMs
) { }
