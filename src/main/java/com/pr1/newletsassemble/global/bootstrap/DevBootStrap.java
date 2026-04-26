package com.pr1.newletsassemble.global.bootstrap;

import com.pr1.newletsassemble.user.domain.Gender;
import com.pr1.newletsassemble.user.domain.Role;
import com.pr1.newletsassemble.user.domain.User;
import com.pr1.newletsassemble.global.time.TimeProvider;
import com.pr1.newletsassemble.user.infra.persistence.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("dev")

public class DevBootStrap implements CommandLineRunner {
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider time;
    @Transactional
    @Override
    public void run(String... args) throws Exception {
        User user = User.of(
                "test1@test.tes",
                passwordEncoder.encode("test1"),
                "010-1111-1111",
                "test1",
                "test1",
                Gender.MAN,
                time.today()
                );
        User admin = User.of(
                "admin@test.tes",
                passwordEncoder.encode("admin"),
                "010-0000-0000",
                "admin",
                "관리자",
                Gender.WOMAN,
                LocalDate.of(1997,12,12));
        admin.updateRole(Role.ADMIN);
        userJpaRepository.saveAll(List.of(user,admin));
    }

}

