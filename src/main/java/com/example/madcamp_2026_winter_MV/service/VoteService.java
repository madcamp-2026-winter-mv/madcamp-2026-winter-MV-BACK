package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.entity.*;
import com.example.madcamp_2026_winter_MV.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private final VoteOptionRepository voteOptionRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void castVote(Long memberId, Long postId, Long optionId) {
        // 1. 이미 투표했는지 확인 (중복 방지)
        if (voteRecordRepository.existsByMemberMemberIdAndPostPostId(memberId, postId)) {
            throw new IllegalStateException("이미 이 투표에 참여하셨습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        VoteOption option = voteOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 선택지입니다."));

        // 2. 투표 기록 저장 (익명성 유지를 위해 누가 뭘 뽑았는지는 DB에만 남음)
        VoteRecord record = VoteRecord.builder()
                .member(member)
                .post(post)
                .voteOption(option)
                .build();
        voteRecordRepository.save(record);

        // 3. 선택지 카운트 증가
        option.setCount(option.getCount() + 1);
    }

    // 투표 옵션 및 결과 조회

    public List<VoteOption> getVoteDetails(Long memberId, Long postId) {
        // 이 게시글에 투표했는지 확인
        boolean hasVoted = voteRecordRepository.existsByMemberMemberIdAndPostPostId(memberId, postId);

        List<VoteOption> options = voteOptionRepository.findByPostId(postId);

        // 투표를 안 했으면 익명성을 위해 결과(count)를 0으로 가려서 반환
        if (!hasVoted) {
            return options.stream()
                    .map(option -> VoteOption.builder()
                            .id(option.getId())
                            .content(option.getContent())
                            .count(0) // 결과 숨김
                            .build())
                    .toList();
        }

        return options;
    }
}