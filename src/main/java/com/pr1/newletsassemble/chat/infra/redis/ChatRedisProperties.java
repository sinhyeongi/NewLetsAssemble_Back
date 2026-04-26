package com.pr1.newletsassemble.chat.infra.redis;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "chat.redis")
public record ChatRedisProperties(
        @Valid @NotNull ChatCache chatCache
) {
    public record ChatCache(
            @Min(1) @Max(20_000) long partyRecentMax,
            @NotNull Duration partyRecentTtl
    ){}
}
