package com.example.madcamp_2026_winter_MV.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomResponseDto {
    private Long chatRoomId;
    private String roomName;
    private Long postId;
    private String postTitle;
    private String createdAt;
    private Integer participantCount;
}