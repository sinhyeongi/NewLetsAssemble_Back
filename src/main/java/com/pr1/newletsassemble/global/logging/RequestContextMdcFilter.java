package com.pr1.newletsassemble.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestContextMdcFilter extends OncePerRequestFilter {
    private static final String HDR_REQUEST_ID = "X-Request-Id";
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = firstNonBlank(request.getHeader(HDR_REQUEST_ID), UUID.randomUUID().toString());
        String ip = resolveClientId(request);
        String ua = safe(request.getHeader("User-Agent"),200);
        MDC.put("requestId",requestId);
        MDC.put("ip",ip);
        MDC.put("ua",ua);
        try{
            filterChain.doFilter(request,response);
        }finally{
            MDC.clear();
        }
    }
    private static String firstNonBlank(String v, String fallback){
        return (v == null || v.isBlank()) ? fallback : v;
    }
    private static String resolveClientId(HttpServletRequest req){
        String xff = req.getHeader("X-Forwarded-For");
        if(xff != null && !xff.isBlank()){
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
    private static String safe(String v , int max){
        if(v == null) return "";
        return v.length() <= max ? v : v.substring(0,max);
    }
}
