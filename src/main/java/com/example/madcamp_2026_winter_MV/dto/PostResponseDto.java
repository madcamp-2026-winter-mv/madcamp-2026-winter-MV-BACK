package com.example.madcamp_2026_winter_MV.dto;

import com.example.madcamp_2026_winter_MV.entity.PostType;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    /** 이메일 기준 작성자 여부(익명 글도 본인에게 수정/삭제 노출). Jackson이 isAuthor()를 "author"로 직렬화해 author 객체와 충돌하므로 명시. */
    @JsonProperty("isAuthor")
    private boolean isAuthor;

    private Integer commentCount;

    // 1. 카테고리 이름
    private String categoryName;

    // 2. 작성 시간
    private String timeAgo;

    // 3. 작성자 객체
    private AuthorDto author;

    // 4. 팟모집 정보 객체
    private PartyInfoDto partyInfo;

    /** 팟 작성자일 때만 존재. 해당 글의 임시 참가자(선택된 댓글 작성자) memberId 목록. */
    private List<Long> tempParticipantIds;

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class CommentResponseDto {
        private Long commentId;
        private Long memberId;
        private String content;
        private String authorNickname;
        private LocalDateTime createdAt;
        private boolean isAnonymous;
        /** 댓글 작성자 분반 ID (익명이면 null, 표시 시 "{roomId} 분반") */
        private Long roomId;
        /** 댓글 작성자 프로필 이미지 (익명이면 null) */
        private String imageUrl;
        /** 현재 로그인 사용자가 댓글 작성자인지 (이메일 기준, 익명 댓글도 수정/삭제 가능). Jackson이 isMine()를 "mine"으로 직렬화할 수 있어 명시. */
        @JsonProperty("isMine")
        private boolean isMine;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AuthorDto {
        private String nickname;
        private boolean isAnonymous;
        private String imageUrl;
        /** 글쓴이 분반 ID (익명이면 null, 표시 시 "{roomId} 분반") */
        private Long roomId;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PartyInfoDto {
        private int currentCount;
        private int maxCount;
        private boolean isRecruiting;
    }
}