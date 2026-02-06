package com.pr1.newletsassemble.user.infra.persistence.jpa;


import com.pr1.newletsassemble.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
