package com.example.madcamp_2026_winter_MV.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 퇴장/강퇴 처리 결과.
 * - targetNickname: 퇴장/강퇴된 멤버의 닉네임
 * - kicked: true=방장이 강퇴, false=본인이 나가기
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveOrKickResult {
    private String targetNickname;
    private boolean kicked;
}
