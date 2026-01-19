package com.example.madcamp_2026_winter_MV.dto;

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

    private boolean isVoted;
    private List<VoteDto.VoteResponse> voteOptions;

    private Integer currentParticipants;
    private Integer maxParticipants;
    private List<CommentResponseDto> comments;
    private int likeCount;
    private boolean isLiked;

    private boolean isAuthor;

    // 1. 카테고리 이름
    private String categoryName;

    // 2. 작성 시간
    private String timeAgo;

    // 3. 작성자 객체
    private AuthorDto author;

    // 4. 팟모집 정보 객체
    private PartyInfoDto partyInfo;

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class CommentResponseDto {
        private Long commentId;
        private Long memberId;
        private String content;
        private String authorNickname;
        private LocalDateTime createdAt;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AuthorDto {
        private String nickname;
        private boolean isAnonymous;
        private String imageUrl;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PartyInfoDto {
        private int currentCount;
        private int maxCount;
        private boolean isRecruiting;
    }
}