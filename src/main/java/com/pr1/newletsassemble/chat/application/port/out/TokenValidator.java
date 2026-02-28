package com.pr1.newletsassemble.chat.application.port.out;


public interface TokenValidator {
    Long validateAndGetUserId(String token);
}
