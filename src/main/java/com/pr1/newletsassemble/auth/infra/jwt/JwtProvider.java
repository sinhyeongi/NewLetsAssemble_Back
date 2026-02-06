package com.pr1.newletsassemble.auth.infra.jwt;

import com.pr1.newletsassemble.global.error.AuthErrorCode;
import com.pr1.newletsassemble.global.time.TimeProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private static final SignatureAlgorithm ALG = SignatureAlgorithm.HS256;
    private final JwtProperties jwtProperties;
    private final TimeProvider timeProvider;
    private Key key;

    @PostConstruct
    void init(){
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }
    /* =================== Create Token ================== */
    private String createToken(Long userId,String role, long expirationMs, JwtTokenType type,String sid,long version,String deviceKey){
        Instant now = timeProvider.now();
        JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expirationMs)))
                .claim(JwtClaim.TYPE.key(),type.name())
                .claim(JwtClaim.VER.key(),version)
                .claim(JwtClaim.SID.key(),sid)
                .claim(JwtClaim.DEVICE_KEY.key(),deviceKey);

        if(role != null){
            builder.claim(JwtClaim.ROLE.key(),role);
        }

        builder.signWith(key, ALG);
        return builder.compact();
    }
    public String createAccessToken(Long userId,String role,String sid,long version,String deviceKey){
        return createToken(userId,role,jwtProperties.accessTokenExpirationMs(),JwtTokenType.ACCESS,sid,version,deviceKey);
    }
    public String createRefreshToken(Long userId,String sid, long version,String deviceKey){
        return createToken(userId,null,jwtProperties.refreshTokenExpirationMs(),JwtTokenType.REFRESH,sid,version,deviceKey);
    }
    /* =================== Token Authentication ================== */
    public AccessTokenAuth authenticateAccess(String token){
        Claims c = parse(token);
        validate(c);
        if(extractType(c) != JwtTokenType.ACCESS){throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code());}
        long userId = requireUserId(c.getSubject());
        long version = requireLong(c, JwtClaim.VER.key());
        String role = requireString(c, JwtClaim.ROLE.key());
        String sid = requireString(c, JwtClaim.SID.key());
        String deviceKey = requireString(c,JwtClaim.DEVICE_KEY.key());
        return new AccessTokenAuth(userId,role,version,sid,deviceKey);
    }
    public RefreshTokenAuth authenticateRefresh(String token){
        Claims c = parse(token);
        validate(c);
        if(extractType(c) != JwtTokenType.REFRESH){throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code());}
        long userId = requireUserId(c.getSubject());
        long version = requireLong(c, JwtClaim.VER.key());
        String sid = requireString(c, JwtClaim.SID.key());
        String deviceKey = requireString(c,JwtClaim.DEVICE_KEY.key());
        return new RefreshTokenAuth(userId,sid,version,deviceKey);
    }
    /* =================== Token parsing ================== */
    private Claims parse(String token){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    /* =================== Token validation ================== */
    private void validate(Claims c){
        requireUserId(c.getSubject());
        extractType(c);
        long version = requireLong(c, JwtClaim.VER.key());
        if(version <= 0){throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code());}
    }
    private JwtTokenType extractType(Claims c){
        String s = getString(c, JwtClaim.TYPE.key());
        if(s == null){throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code());}
        try{
            return JwtTokenType.valueOf(s);
        }catch(IllegalArgumentException e){
            throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code(),e);
        }
    }
    /* =================== Safe Claim Extraction ================== */
    private long requireUserId(String subject){
        if(subject == null){throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code());}
        try{
            return Long.parseLong(subject);
        }catch(NumberFormatException e){
            throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code(),e);
        }
    }
    private String requireString(Claims c, String key){
        String s = getString(c,key);
        if(s == null || s.isBlank()){throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code());}
        return s;
    }
    private String getString(Claims c, String key){
        try{
            Object o = c.get(key);
            if(o == null){return null;}
            if(o instanceof String s){return s;}
            throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code());
        }catch(ClassCastException e){
            throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code(),e);
        }
    }
    private long requireLong(Claims c, String key){
        try{
            Object o = c.get(key);
            if(o == null){throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code());}
            if(o instanceof Number n){return n.longValue();}
            if(o instanceof String s){return Long.parseLong(s);}
            throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code());
        }catch(NumberFormatException e){
            throw new JwtException(AuthErrorCode.AUTH_TOKEN_INVALID.code(),e);
        }
    }
}
