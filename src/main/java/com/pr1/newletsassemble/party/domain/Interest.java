package com.pr1.newletsassemble.party.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "interests",
         uniqueConstraints = @UniqueConstraint( name = "uk_interest_name", columnNames = "name")
)
public class Interest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_id")
    private Long id;
    @Column(nullable = false, length = 30)
    private String name;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    public Interest(String name){
        this.name = name;
    }
}
