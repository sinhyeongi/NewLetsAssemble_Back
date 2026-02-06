package com.pr1.newletsassemble.auth.infra.security;

import com.pr1.newletsassemble.auth.application.SessionActiveService;
import com.pr1.newletsassemble.auth.infra.jwt.AccessTokenAuth;
import com.pr1.newletsassemble.auth.infra.jwt.JwtProperties;
import com.pr1.newletsassemble.auth.infra.jwt.JwtProvider;
import com.pr1.newletsassemble.global.error.AuthErrorCode;
import com.pr1.newletsassemble.global.time.TimeProvider;
import com.pr1.newletsassemble.auth.infra.redis.repository.TokenVersionRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwt;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenVersionRepository tokenVersionRepository;
    private final JwtAuthEntryPoint jwtAuthEntryPoint; //401
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler; //403 ( 정지 유저 포함 )
    private final TimeProvider timeProvider;
    private final JwtProperties jwtProperties;
    private final SessionActiveService sessionActiveService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            /* =================== Authenticate Access Token ================== */
            AccessTokenAuth auth = jwt.authenticateAccess(token);
            String deviceKey = resolveDeviceKey(request);
            if(deviceKey == null || auth.deviceKey() == null || !deviceKey.equals(auth.deviceKey())){
                throw new AuthFailureException(AuthErrorCode.AUTH_DEVICE_KEY_MISSING);
            }
            String activeSid = sessionActiveService
                    .getActiveSid(auth.userId(),deviceKey,Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()))
                    .orElseThrow(() -> new AuthFailureException(AuthErrorCode.AUTH_DEVICE_SESSION_REVOKED));
            if(!activeSid.equals(auth.sid())){
                throw new AuthFailureException(AuthErrorCode.AUTH_DEVICE_SESSION_REVOKED);
            }
            /* =================== Token revoked check (ver) ================== */
            long currentVersion = tokenVersionRepository.getOrInit(auth.userId(), Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()).plusDays(3));
            if (currentVersion != auth.tokenVersion()) {
                throw new AuthFailureException(AuthErrorCode.AUTH_DEVICE_SESSION_REVOKED);
            }
            UserDetails user = customUserDetailsService.loadUserById(auth.userId());
            if(user instanceof CustomUserDetails cud && cud.isSuspended(timeProvider.now())){
                forbidden(request,response,AuthErrorCode.AUTH_ACCOUNT_SUSPENDED);
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request,response);
        }catch(ExpiredJwtException e){
            // JWT (ACCESS TOKEN) 만료 Entrypoint에서 처리 (401)
            unauthorized(request,response,new AuthFailureException(AuthErrorCode.AUTH_ACCESS_EXPIRED,e));
        }catch(AuthFailureException e) {
            unauthorized(request,response,e);
        }catch(JwtException | IllegalArgumentException e){
            // 서명 위조, claim 변조 , 타입 오류 등 Entrypoint에서 처리 (401)
            unauthorized(request,response,new AuthFailureException(AuthErrorCode.AUTH_ACCESS_INVALID,e));
        }
    }

    /* =================== Helpers ================== */
    private void unauthorized(HttpServletRequest request, HttpServletResponse response, AuthFailureException ex) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        jwtAuthEntryPoint.commence(request, response, ex);
    }
    private void forbidden(HttpServletRequest req,HttpServletResponse res, AuthErrorCode code) throws IOException{
        SecurityContextHolder.clearContext();
        jwtAccessDeniedHandler.handle(req,res,new AccessDeniedException(code.code()));
    }
    /**
     * jwt 토큰 해더 확인 및 토큰 값 가져오기
     *
     * @param request
     * @return 토큰 값 or null
     */
    private String resolveToken(HttpServletRequest request){
        String header = request.getHeader("Authorization");
        if(header == null || !header.startsWith("Bearer ")) {return null;}
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
    private String resolveDeviceKey(HttpServletRequest req){
        String header = req.getHeader("X-LA-Device-Id");
        if(header != null && !header.isBlank()){return header.trim();}
        if(req.getCookies() == null){return null;}
        for(var c : req.getCookies()){
            if("X-LA-Device-Id".equals(c.getName())){
                String v = c.getValue();
                return (v == null || v.isBlank()) ? null : v.trim();
            }
        }
        return null;
    }
}
