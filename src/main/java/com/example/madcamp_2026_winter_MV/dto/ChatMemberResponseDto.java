package com.example.madcamp_2026_winter_MV.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemberResponseDto {
    private Long memberId;
    private String nickname;
    private String profileImage;
    /** 해당 멤버가 이 채팅방(팟)의 방장(게시글 작성자)인지 */
    @JsonProperty("isOwner")
    private boolean owner;
}
