package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Room;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void joinRoomByEmail(String email, String inviteCode) {
        Room room = roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 초대코드입니다."));
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        room.addMember(member);
    }

    @Transactional
    public void joinRoom(Long memberId, String inviteCode) {
        Room room = roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 초대코드입니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        room.addMember(member);
    }

    // [기본] 발표자 랜덤 선정 로직
    @Transactional
    public Member pickPresenter(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        List<Member> members = room.getMembers();
        if (members.isEmpty()) {
            throw new IllegalStateException("분반에 멤버가 없습니다.");
        }

        // 발표 횟수가 가장 적은 사람들 추출
        int minCount = members.stream()
                .mapToInt(Member::getPresentationCount)
                .min().orElse(0);

        List<Member> candidates = members.stream()
                .filter(m -> m.getPresentationCount() == minCount)
                .toList();

        Member selected = candidates.get(new Random().nextInt(candidates.size()));

        // 선정된 멤버 횟수 증가 및 룸에 현재 발표자로 등록
        selected.setPresentationCount(selected.getPresentationCount() + 1);
        room.setCurrentPresenterId(selected.getMemberId());

        return selected;
    }

    // 오늘 발표자가 다음 사람을 뽑는 로직 (또는 운영진이 재선정)
    @Transactional
    public Member pickNextPresenter(Long roomId, boolean isHandover) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        if (!isHandover) {
            // 운영진이 그냥 재선정하는 경우: 기존 발표자 횟수 복구(취소)
            if (room.getCurrentPresenterId() != null) {
                memberRepository.findById(room.getCurrentPresenterId()).ifPresent(m -> {
                    if (m.getPresentationCount() > 0) m.setPresentationCount(m.getPresentationCount() - 1);
                });
            }
        }

        // 새로운 다음 발표자 선정
        return pickPresenter(roomId);
    }
}