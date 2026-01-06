package com.pr1.newletsassemble.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    @Bean
    WebSecurityCustomizer webSecurityCustomizer(){
        return (web -> {
            web.ignoring().requestMatchers("/resources/**","/favicon.ico");
        });
    }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity security){
        security.csrf(AbstractHttpConfigurer::disable);
        security.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return security.build();
    }
}
