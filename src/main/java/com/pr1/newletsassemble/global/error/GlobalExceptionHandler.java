package com.pr1.newletsassemble.global.error;

import com.pr1.newletsassemble.auth.application.AuthCookieWriter;
import com.pr1.newletsassemble.global.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final AuthCookieWriter cookieWriter;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handle(ApiException e,HttpServletResponse res){
        AuthErrorCode code = (AuthErrorCode) e.code();
        if(code == AuthErrorCode.AUTH_DEVICE_KEY_MISMATCH_LOGOUT_ALL ||
        code == AuthErrorCode.AUTH_REFRESH_REUSE_DETECTED ||
        code == AuthErrorCode.AUTH_REFRESH_EXPIRED){
            cookieWriter.clearRefreshCookies(res);
        }
        return ResponseEntity.status(e.code().status())
                .body(new ErrorResponse(e.code().code(),e.getMessage()));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handle(IllegalStateException e){
        return ResponseEntity
                .status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR","Internal Server Error"));
    }
}
