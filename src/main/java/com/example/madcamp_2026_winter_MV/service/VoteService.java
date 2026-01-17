package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.entity.*;
import com.example.madcamp_2026_winter_MV.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public void castVote(String email, Long postId, Long optionId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 1. 작성자가 수동으로 종료했는지 확인 (Post에 isClosed 필드가 있다고 가정)
        // 2. 혹은 생성된지 24시간이 지났는지 확인
        if (post.isClosed() || (post.getCreatedAt() != null && post.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24)))) {
            throw new IllegalStateException("이미 마감된 투표입니다.");
        }

        // 중복 투표 확인
        if (voteRecordRepository.existsByMemberMemberIdAndPostPostId(member.getMemberId(), postId)) {
            throw new IllegalStateException("이미 이 투표에 참여하셨습니다.");
        }

        VoteOption option = voteOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 선택지입니다."));

        VoteRecord record = VoteRecord.builder()
                .member(member)
                .post(post)
                .voteOption(option)
                .build();
        voteRecordRepository.save(record);

        option.setCount(option.getCount() + 1);
    }

    // 작성자가 직접 투표를 종료하는 기능 추가
    @Transactional
    public void closeVote(String email, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 글쓴이 본인인지 확인
        if (!post.getMember().getEmail().equals(email)) {
            throw new IllegalStateException("작성자만 투표를 종료할 수 있습니다.");
        }

        post.setClosed(true); // Post 엔티티에 @Setter 혹은 close() 메서드 필요
    }

    public List<VoteOption> getVoteDetails(String email, Long postId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        boolean hasVoted = voteRecordRepository.existsByMemberMemberIdAndPostPostId(member.getMemberId(), postId);

        // 투표 결과 공개 조건: 사용자가 투표했거나, 작성자가 마감했거나, 생성된지 24시간이 지난 경우
        boolean showResult = hasVoted || post.isClosed() || (post.getCreatedAt() != null && post.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24)));

        List<VoteOption> options = voteOptionRepository.findByPostPostId(postId);

        if (!showResult) {
            // 결과 숨김 처리가 필요한 경우: count를 0으로 설정한 가짜 객체 리스트 반환
            return options.stream()
                    .map(option -> {
                        VoteOption dummy = new VoteOption();
                        dummy.setId(option.getId());
                        dummy.setContent(option.getContent());
                        dummy.setCount(0); // 결과 숨김
                        return dummy;
                    })
                    .toList();
        }

        return options;
    }
}