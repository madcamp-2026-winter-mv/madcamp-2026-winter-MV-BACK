package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.MemberResponseDto;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.PostType;
import com.example.madcamp_2026_winter_MV.repository.PostRepository;
import com.example.madcamp_2026_winter_MV.repository.CommentRepository;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public MemberResponseDto getMyInfo(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        // 통계 데이터 계산
        long writtenPosts = postRepository.countByMember(member);
        long commentedPosts = commentRepository.countDistinctPostByMember(member);
        long ongoingParties = postRepository.countByTypeAndMember(PostType.PARTY, member);

        return MemberResponseDto.builder()
                .nickname(member.getNickname())
                .realName(member.getRealName())
                .email(member.getEmail())
                .roomName(member.getRoom() != null ? member.getRoom().getName() : "소속 없음")
                .role("MEMBER")
                .presentationCount(2)
                .attendanceRate(95.0)
                .writtenPostsCount(writtenPosts)
                .commentedPostsCount(commentedPosts)
                .ongoingPartyCount(ongoingParties)
                .allowAlarm(true)
                .build();
    }


    @Transactional
    public void updateNickname(String email, String newNickname) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        if (memberRepository.existsByNickname(newNickname)) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }

        member.setNickname(newNickname);
    }

    @Transactional
    public boolean updateAlarmStatus(String email, boolean allowAlarm) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        member.setAllowAlarm(allowAlarm);
        return member.isAllowAlarm();
    }


    @Transactional
    public void leaveRoom(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        member.setRoom(null);
    }
}