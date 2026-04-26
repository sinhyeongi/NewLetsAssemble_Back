package com.pr1.newletsassemble.auth.infra.security;

import com.pr1.newletsassemble.user.infra.persistence.jpa.UserJpaRepository;
import com.pr1.newletsassemble.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService, UserDetailsService {
    private final UserJpaRepository userJpaRepository;
    @Override
    public CustomUserDetails loadUserByUsername(String email) {
        User user = userJpaRepository.findByEmail(email)
                .orElseThrow( () -> new UsernameNotFoundException("User Not Found"));

        return CustomUserDetails.from(user);
    }

    @Override
    public UserDetails loadUserById(Long userId) {
        User user = userJpaRepository.findById(userId).orElseThrow( () -> new UsernameNotFoundException("User Not Found"));
        return CustomUserDetails.from(user);
    }
}
