package com.pr1.newletsassemble.user.domain.policy;

import java.time.LocalDate;
import java.time.Year;

public final class AgePolicy {
    private AgePolicy(){}

    /**
     *  2/29일 출생자의 "해당 연도 생일" 정책대로 계산
     *  - 윤년 : 2/29
     *  - 평년 : 3/1
     */
    public static LocalDate birthdayInYear(LocalDate birthDate,int year){
        if(birthDate.getMonthValue() == 2 && birthDate.getDayOfMonth() == 29){
            return Year.isLeap(year) ? LocalDate.of(year,2,29) : LocalDate.of(year,3,1);
        }
        // 일반 케이스 : 같은 월/일을 해당 연도로 이
        return LocalDate.of(year,birthDate.getMonthValue(),birthDate.getDayOfMonth());
    }

    /**
     * 만나이 계산 ( 생일 기준 )
     */
    public static int manAge(LocalDate birthDate, LocalDate today){
        int age = today.getYear() - birthDate.getYear();
        LocalDate birthdayThisYear = birthdayInYear(birthDate,today.getYear());
        if(today.isBefore(birthdayThisYear)){
            age--;
        }
        return Math.max(age,0);
    }

    /**
     *  "만 X세 이상" 판정
     */
    public static boolean isAtLeast(LocalDate birthDate, LocalDate today, int minAge){
        return manAge(birthDate,today) >= minAge;
    }
}
