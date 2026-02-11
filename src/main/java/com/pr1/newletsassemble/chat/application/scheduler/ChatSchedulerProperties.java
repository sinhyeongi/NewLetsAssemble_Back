package com.pr1.newletsassemble.chat.application.scheduler;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chat")
@Getter
public class ChatSchedulerProperties {
    private int scheduleLimit;
}
