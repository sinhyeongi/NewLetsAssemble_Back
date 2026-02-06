package com.pr1.newletsassemble.auth.api;

import com.pr1.newletsassemble.auth.api.dto.*;
import com.pr1.newletsassemble.auth.application.AuthCookieWriter;
import com.pr1.newletsassemble.auth.application.AuthService;
import com.pr1.newletsassemble.auth.api.dto.*;
import com.pr1.newletsassemble.global.api.ApiResponse;
import com.pr1.newletsassemble.auth.infra.http.device.DeviceInfoResolver;
import com.pr1.newletsassemble.auth.infra.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final DeviceInfoResolver resolver;
    private final AuthCookieWriter cookieWriter;
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest req, HttpServletResponse res){
        LoginTokens loginResponse = authService.login(loginRequest,resolver.resolve(req));
        cookieWriter.setLoginCookie(res,loginResponse.refreshToken(),loginResponse.deviceKey());
        return ApiResponse.ok(new LoginResponse(loginResponse.accessToken()));
    }
    @PostMapping("/reissue")
    public ApiResponse<ReissueResponse> reissue(HttpServletRequest req,
                                                HttpServletResponse res,
                                                @CookieValue(name = "refresh", required = false)String refresh){
        ReissueTokens reissueResponse = authService.reissue(refresh,resolver.resolve(req),res);
        cookieWriter.setRefreshCookie(res,reissueResponse.newRefresh());
        return ApiResponse.ok(new ReissueResponse(reissueResponse.access()));
    }
    @PostMapping("/logout")
    public ApiResponse<ResponseEntity<Void>> logout(@Valid @RequestBody LogoutRequest request){
        authService.logOut(request);
        return ApiResponse.ok(ResponseEntity.noContent().build());
    }
    @PostMapping("/logout-all")
    public ApiResponse<ResponseEntity<Void>> logoutALL(@AuthenticationPrincipal CustomUserDetails user,HttpServletResponse res){
        authService.logOutALL(user.getUserid());
        cookieWriter.clearRefreshCookies(res);
        return ApiResponse.ok(ResponseEntity.noContent().build());
    }
}
