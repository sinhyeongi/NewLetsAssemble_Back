package com.pr1.newletsassemble.party.domain;

import com.pr1.newletsassemble.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "parties",
        indexes = {
        @Index(name = "idx_party_host_user_id", columnList = "host_user_id"),
                @Index(name = "idx_party_created_at", columnList = "created_at")
        }
)
@Getter
public class Party {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long id;
    @JoinColumn(name = "host_user_id",nullable = false)
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    private User host; // 파티 생성자
    @Column(nullable = false, length = 100)
    private String title; // 파티 이름
    @Column(name = "is_online",nullable = false)
    private boolean online; // 온라인 여부
    @Column(nullable = false,length = 50)
    private String area; // 지역
    @Lob
    private String content; //모집글 내용
    @Column(nullable = false,updatable = false,name = "created_at")
    private Instant createdAt; // 생성일
    @Column(nullable = false)
    private int personnel; //모집 인원
    @Column(length = 200)
    private String notification; // 전체 공지
    private Party(User host,String title,boolean online,
                  String area,String content,Instant createAt,int personnel,String notification){
        this.host = host;
        this.title = title;
        this.online = online;
        this.area = area;
        this.content = content;
        this.createdAt = createAt;
        this.personnel = personnel;
        this.notification = notification;
    }
    public static Party create(User host,String title,boolean online,String area,String content,Instant createdAt,int personnel,String notification){
        return new Party(host,title,online,area,content,createdAt,personnel,notification);
    }
}
