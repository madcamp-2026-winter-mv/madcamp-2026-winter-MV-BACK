package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.MemberResponseDto;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.PostType;
import com.example.madcamp_2026_winter_MV.entity.Room;
import com.example.madcamp_2026_winter_MV.repository.PostRepository;
import com.example.madcamp_2026_winter_MV.repository.CommentRepository;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.repository.RoomRepository;
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
    private final RoomRepository roomRepository;

    public MemberResponseDto getMyInfo(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        // 통계 데이터 계산
        long writtenPosts = postRepository.countByMember(member);
        long commentedPosts = commentRepository.countDistinctPostByMember(member);
        long ongoingParties = postRepository.countByTypeAndMember(PostType.PARTY, member);

        // 출석률 계산
        double attendanceRate = 0.0;
        if (member.getRoom() != null && member.getRoom().getTotalSessionCount() > 0) {
            attendanceRate = ((double) member.getAttendanceCount() / member.getRoom().getTotalSessionCount()) * 100;
        }

        return MemberResponseDto.builder()
                .nickname(member.getNickname())
                .realName(member.getRealName())
                .email(member.getEmail())
                .profileImage(member.getProfileImage())
                .roomId(member.getRoom() != null ? member.getRoom().getRoomId() : null)
                .roomName(member.getRoom() != null ? member.getRoom().getName() : "소속 없음")
                .role(member.getRole().name())
                .presentationCount(member.getPresentationCount())
                .attendanceRate(attendanceRate)
                .writtenPostsCount(writtenPosts)
                .commentedPostsCount(commentedPosts)
                .ongoingPartyCount(ongoingParties)
                .allowAlarm(member.isAllowAlarm())
                .build();
    }

    @Transactional
    public void updateNickname(String email, String newNickname) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        if (memberRepository.existsByNickname(newNickname)) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }

        member.updateNickname(newNickname);
    }

    @Transactional
    public boolean updateAlarmStatus(String email, boolean allowAlarm) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        member.updateAlarm(allowAlarm);
        return member.isAllowAlarm();
    }

    @Transactional
    public void leaveRoom(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        member.setRoom(null);
    }

    //  분반 코드 입장 로직
    @Transactional
    public void joinRoomByCode(String email, String inviteCode) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        // 초대 코드로 방을 찾음
        Room room = roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 코드입니다."));

        // 멤버의 소속 분반 업데이트
        member.setRoom(room);
    }
}