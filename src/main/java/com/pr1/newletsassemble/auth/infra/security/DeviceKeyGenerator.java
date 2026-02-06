package com.pr1.newletsassemble.auth.infra.security;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeviceKeyGenerator {
    public String newKey(){
        return UUID.randomUUID().toString().replace("-","");
    }
}
