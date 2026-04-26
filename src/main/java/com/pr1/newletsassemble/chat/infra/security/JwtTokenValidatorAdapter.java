package com.pr1.newletsassemble.chat.infra.security;

import com.pr1.newletsassemble.chat.application.port.out.TokenValidator;
import com.pr1.newletsassemble.global.security.jwt.AccessTokenAuth;
import com.pr1.newletsassemble.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenValidatorAdapter implements TokenValidator {
    private final JwtProvider jwtProvider;

    @Override
    public Long validateAndGetUserId(String token) {
        AccessTokenAuth auth = jwtProvider.authenticateAccess(token);
        if(auth == null){
            return 0L;
        }
        return auth.userId();
    }
}
