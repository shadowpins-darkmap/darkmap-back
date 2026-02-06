package com.sp.member.util;

import java.util.Random;

public class NicknameGenerator {
    private static final String[] A = {
            "붉은", "푸른", "녹색", "진홍", "금빛", "은빛", "하얀", "검은"
    };

    private static final String[] B = {
            "태양", "달", "별", "번개", "오로라", "불꽃", "용암", "유성",
            "수정", "이슬", "눈송이", "빙하", "모래알", "물방울",
            "혜성", "노을", "빗줄기", "서리"
    };

    private static final Random random = new Random();

    public static String generateNickname(int userNumber) {
        String partA = A[random.nextInt(A.length)];
        String partB = B[random.nextInt(B.length)];
        return "빛나는 " + partA + " " + partB + "의 핀 " + userNumber;
    }
}
