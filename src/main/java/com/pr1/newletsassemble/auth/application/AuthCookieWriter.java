package com.pr1.newletsassemble.auth.application;

import com.pr1.newletsassemble.auth.infra.jwt.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieWriter {
    private final JwtProperties jwtProperties;

    public void setLoginCookie(HttpServletResponse res, String refreshToken, String deviceKey){
        if(refreshToken != null) setRefreshCookie(res,refreshToken);
        if(deviceKey != null) setDeviceKeyCookie(res,deviceKey);
    }
    public void setRefreshCookie(HttpServletResponse res, String refreshToken){
        ResponseCookie rc = ResponseCookie.from("refresh",refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()))
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE,rc.toString());
    }
    public void setDeviceKeyCookie(HttpServletResponse res, String deviceKey){
        ResponseCookie rc = ResponseCookie.from("X-LA-Device-Id",deviceKey)
                .httpOnly(false)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(365))
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE,rc.toString());
    }
    public void clearRefreshCookies(HttpServletResponse res){
        ResponseCookie rc = ResponseCookie.from("refresh","")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE,rc.toString());
    }
}
