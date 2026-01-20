package com.example.madcamp_2026_winter_MV.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class ChatMessageDto {
    private Long chatRoomId;
    private String senderNickname;
    /** 발신자 프로필 이미지 URL (채팅방 멤버에서 닉네임으로 매칭, 없으면 null) */
    private String senderProfileImageUrl;
    private String content;
    private String timestamp; // "2026-01-17T16:44:26" 형태 또는 포맷팅된 문자열
}