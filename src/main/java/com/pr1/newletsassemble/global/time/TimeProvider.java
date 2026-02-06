package com.pr1.newletsassemble.global.time;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TimeProvider {
    private final Clock clock;

    public Instant now(){
        return Instant.now(clock);
    }
    public LocalDate today(){
        return LocalDate.now(clock);
    }
    public LocalDateTime nowDateTime(){
        return LocalDateTime.now(clock);
    }
}
