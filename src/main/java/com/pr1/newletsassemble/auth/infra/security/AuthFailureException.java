package com.pr1.newletsassemble.auth.infra.security;

import com.pr1.newletsassemble.global.error.AuthErrorCode;
import org.springframework.security.core.AuthenticationException;

public class AuthFailureException extends AuthenticationException {
    private final AuthErrorCode code;
    public AuthFailureException(AuthErrorCode code, Throwable cause){
        super(code.code(), cause);
        this.code = code;
    }
    public AuthFailureException(AuthErrorCode code){
        super(code.code());
        this.code = code;
    }
    public AuthErrorCode getCode(){
        return code;
    }
}
