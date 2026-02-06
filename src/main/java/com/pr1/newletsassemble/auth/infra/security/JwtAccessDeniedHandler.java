package com.pr1.newletsassemble.auth.infra.security;


import com.pr1.newletsassemble.global.api.ApiResponse;
import com.pr1.newletsassemble.global.error.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        AuthErrorCode auth = AuthErrorCode.AUTH_FORBIDDEN;

        if(AuthErrorCode.AUTH_ACCOUNT_SUSPENDED.code().equals(accessDeniedException.getMessage())){
            auth = AuthErrorCode.AUTH_ACCOUNT_SUSPENDED;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(auth.code(),auth.message()));
    }
}
