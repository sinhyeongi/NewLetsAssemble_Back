package com.pr1.newletsassemble.auth.infra.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface CustomUserDetailsService{
    UserDetails loadUserById(Long userId);
}
