package com.example.madcamp_2026_winter_MV.dto;

import com.example.madcamp_2026_winter_MV.entity.Member;
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
    private Long roomId;
    private String role; // OWNER, ADMIN, MEMBER

    private int presentationCount;    // 발표 횟수
    private int attendanceCount; //출석 횟수
    private double attendanceRate;     // 출석률 (%)
    private long writtenPostsCount;    // 내가 쓴 글
    private long commentedPostsCount;  // 댓글 단 글
    private long ongoingPartyCount;    // 진행 중인 팟
    private boolean allowAlarm;        // 알림 설정 상태

    public static MemberResponseDto from(Member member) {
        if (member == null) return null;

        return MemberResponseDto.builder()
                .nickname(member.getNickname())
                .realName(member.getRealName())
                .email(member.getEmail())
                .roomId(member.getRoom() != null ? member.getRoom().getRoomId() : null)
                .roomName(member.getRoom() != null ? member.getRoom().getName() : null)
                .role(member.getRole() != null ? member.getRole().name() : "USER")
                .presentationCount(member.getPresentationCount())
                .attendanceCount(member.getAttendanceCount()) // 필요시 추가
                .allowAlarm(member.isAllowAlarm())
                .writtenPostsCount(member.getPosts() != null ? member.getPosts().size() : 0)
                .build();
    }
}