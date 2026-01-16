package com.example.madcamp_2026_winter_MV.dto;

import lombok.*;
import java.util.List;

public class VoteDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VoteRequest {
        private Long memberId; // 투표하는 사람
        private Long optionId; // 선택한 옵션 ID
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VoteResponse {
        private Long optionId;
        private String content;
        private Integer count;
        private Double percentage; // 계산된 득표율 (UI용)
    }
}