package com.example.madcamp_2026_winter_MV.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponseDto {
    private Long commentId;
    private String content;
    private String authorNickname;
    private LocalDateTime createdAt;
}