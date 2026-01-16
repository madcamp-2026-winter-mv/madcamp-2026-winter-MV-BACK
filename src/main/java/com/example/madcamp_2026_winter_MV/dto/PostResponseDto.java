package com.example.madcamp_2026_winter_MV.dto;

import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.entity.PostType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PostResponseDto {
    private Long postId;
    private String title;
    private String content;
    private PostType type;
    private String authorNickname;
    private LocalDateTime createdAt;

    // 투표 관련 정보
    private boolean isVoted;
    private List<VoteDto.VoteResponse> voteOptions; // 투표 항목들 (ID, 내용, 득표수 포함)

    // 팟모집 관련 정보
    private Integer currentParticipants;
    private Integer maxParticipants;
}