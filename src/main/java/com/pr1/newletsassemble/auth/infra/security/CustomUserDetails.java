package com.pr1.newletsassemble.auth.infra.security;

import com.pr1.newletsassemble.user.domain.Role;
import com.pr1.newletsassemble.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final Long userId;
    private final String email;
    private final String password;
    private final Role role;
    private final Instant suspendedUntil;

    public static CustomUserDetails from(User user){
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getSuspendedUntil()
        );
    }
    public boolean isSuspended(Instant now){
        return suspendedUntil != null && suspendedUntil.isAfter(now);
    }
    public Long getUserid(){
        return userId;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
