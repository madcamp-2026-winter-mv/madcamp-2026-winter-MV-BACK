package com.example.madcamp_2026_winter_MV.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberResponseDto {
    private String nickname;
    private String realName;
    private String email;
    private String roomName;
    private String role; // OWNER, ADMIN, MEMBER

    private int presentationCount;    // 발표 횟수
    private double attendanceRate;     // 출석률 (%)
    private long writtenPostsCount;    // 내가 쓴 글
    private long commentedPostsCount;  // 댓글 단 글
    private long ongoingPartyCount;    // 진행 중인 팟
    private boolean allowAlarm;        // 알림 설정 상태
}