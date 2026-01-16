package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.VoteDto;
import com.example.madcamp_2026_winter_MV.entity.VoteOption;
import com.example.madcamp_2026_winter_MV.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    // 투표하기 실행
    @PostMapping("/{postId}")
    public ResponseEntity<String> castVote(
            @PathVariable Long postId,
            @RequestBody VoteDto.VoteRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        // 로그인된 사용자의 이메일 가져오기
        String email = principal.getAttribute("email");

        voteService.castVote(email, postId, request.getOptionId());
        return ResponseEntity.ok("투표가 성공적으로 완료되었습니다.");
    }

    // 투표 결과 및 옵션 조회
    @GetMapping("/{postId}")
    public ResponseEntity<List<VoteDto.VoteResponse>> getVoteDetails(
            @PathVariable Long postId,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        List<VoteOption> options = voteService.getVoteDetails(email, postId);

        // 전체 투표 수 계산 (득표율 계산용)
        int totalVotes = options.stream().mapToInt(VoteOption::getCount).sum();

        // Entity를 DTO로 변환하여 반환
        List<VoteDto.VoteResponse> response = options.stream()
                .map(option -> VoteDto.VoteResponse.builder()
                        .optionId(option.getId())
                        .content(option.getContent())
                        .count(option.getCount())
                        .percentage(totalVotes == 0 ? 0.0 : (double) option.getCount() / totalVotes * 100)
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
}