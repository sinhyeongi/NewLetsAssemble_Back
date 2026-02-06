package com.pr1.newletsassemble.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_last_login", columnList = "last_login"),
                @Index(name = "idx_user_suspendedUntil", columnList = "suspended_until")

        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    @Column(unique = true,nullable = false)
    private String email; // 이메일 유저 아이디 값
    @Column(nullable = false)
    private String password; // 비밀번호
    @Column(nullable = false)
    private String phone; // 휴대폰 번호
    @Column(nullable = false)
    private String name; // 이름
    @Column(nullable = false, unique = true,name = "nick_name")
    private String nickname; // 닉네임

    @Column(name = "last_login")
    private Instant lastLogin; // 마지막 로그인 날
    @Column(name = "suspended_until")
    private Instant suspendedUntil; // 정지 기간
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @ColumnDefault("0")
    private int point;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
    @Column(name = "birth_date",nullable = false)
    private LocalDate birthDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private User(String email, String encodedPassword, String phone, String name, String nickname, Gender gender, LocalDate birthDate){
        this.email = email;
        this.password = encodedPassword;
        this.phone = phone;
        this.name = name;
        this.nickname = nickname;
        this.gender = gender;
        this.birthDate = birthDate;
        this.role = Role.USER;
    }
    public static User of(String email, String encodedPassword, String phone, String name, String nickname, Gender gender, LocalDate birthDate){
        return new User(email,encodedPassword,phone,name,nickname,gender,birthDate);
    }

    public void updateRole(Role role){
        this.role = role;
    }
    public void recordLogin(Instant now){
        this.lastLogin = now;
    }
    public boolean isSuspended(Instant now){
        return suspendedUntil != null && suspendedUntil.isAfter(now);
    }
    public boolean isDeleted(){
        return deletedAt != null;
    }
}
