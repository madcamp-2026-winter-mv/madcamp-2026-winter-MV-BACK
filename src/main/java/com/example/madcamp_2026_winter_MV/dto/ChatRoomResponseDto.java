package com.example.madcamp_2026_winter_MV.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomResponseDto {
    private Long chatRoomId;
    private String roomName;
    private Long postId;
    private String postTitle;
    /** 채팅방 개설자(글쓴이/방장) 프로필 이미지 URL. 대표 이미지용 */
    private String creatorProfileImageUrl;
    private String createdAt;
    private Integer participantCount;
    private int unreadCount;
}