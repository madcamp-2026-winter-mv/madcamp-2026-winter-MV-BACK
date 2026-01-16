package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.VoteDto;
import com.example.madcamp_2026_winter_MV.entity.VoteOption;
import com.example.madcamp_2026_winter_MV.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    // 투표하기 실행 - POST /api/vote/{postId}
    @PostMapping("/{postId}")
    public ResponseEntity<String> castVote(
            @PathVariable Long postId,
            @RequestBody VoteDto.VoteRequest request) {

        voteService.castVote(request.getMemberId(), postId, request.getOptionId());
        return ResponseEntity.ok("투표가 성공적으로 완료되었습니다.");
    }

    // 투표 결과 및 옵션 조회 - GET /api/vote/{postId}?memberId=1
    @GetMapping("/{postId}")
    public ResponseEntity<List<VoteDto.VoteResponse>> getVoteDetails(
            @PathVariable Long postId,
            @RequestParam Long memberId) {

        List<VoteOption> options = voteService.getVoteDetails(memberId, postId);

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