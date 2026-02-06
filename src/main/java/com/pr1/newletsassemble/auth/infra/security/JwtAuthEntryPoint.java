package com.pr1.newletsassemble.auth.infra.security;


import com.pr1.newletsassemble.auth.application.AuthCookieWriter;
import com.pr1.newletsassemble.global.api.ApiResponse;
import com.pr1.newletsassemble.global.error.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    private final AuthCookieWriter cookieWriter;
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        AuthErrorCode code = AuthErrorCode.AUTH_UNAUTHORIZED;

        if(authException instanceof AuthFailureException afe){
            code = afe.getCode();
        }

        if(code == AuthErrorCode.AUTH_DEVICE_KEY_MISMATCH_LOGOUT_ALL){
            cookieWriter.clearRefreshCookies(response);
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code.code(),code.message()));
    }

}
