package com.pr1.newletsassemble.auth.infra.security;

import com.pr1.newletsassemble.auth.application.SessionActiveService;
import com.pr1.newletsassemble.auth.infra.jwt.JwtProperties;
import com.pr1.newletsassemble.auth.infra.jwt.JwtProvider;
import com.pr1.newletsassemble.global.time.TimeProvider;
import com.pr1.newletsassemble.auth.infra.redis.repository.TokenVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenVersionRepository tokenVersionRepository;
    private final TimeProvider timeProvider;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtProperties jwtProperties;
    private final SessionActiveService sessionActiveService;
    @Bean
    WebSecurityCustomizer webSecurityCustomizer(){
        return (web -> {
            web.ignoring().requestMatchers("/resources/**","/favicon.ico");
        });
    }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity security){
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtProvider,userDetailsService,tokenVersionRepository,jwtAuthEntryPoint,jwtAccessDeniedHandler,timeProvider,jwtProperties,sessionActiveService);
        security
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement( s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling( ex -> ex
                                .authenticationEntryPoint(jwtAuthEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler)
                        )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login","/api/auth/reissue").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        //Test
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        filter, UsernamePasswordAuthenticationFilter.class
                );

        return security.build();
    }
}
