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

    /**
     * 초대 코드로 분반 입장하기
     */
    @Transactional
    public void joinRoom(Long memberId, String inviteCode) {
        Room room = roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 초대코드입니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        member.setRoom(room); // 멤버에 분반 정보 매핑
    }

    /**
     * 가중치 기반 발표자(제물) 선정
     */
    @Transactional
    public Member pickPresenter(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        // 해당 분반 멤버 중 발표 횟수가 적은 순으로 가져옴
        List<Member> members = room.getMembers();

        // 간단한 가중치 로직: 발표 횟수가 가장 적은 멤버들 중 랜덤 선정
        int minCount = members.stream()
                .mapToInt(Member::getPresentationCount)
                .min().orElse(0);

        List<Member> candidates = members.stream()
                .filter(m -> m.getPresentationCount() == minCount)
                .toList();

        Member selected = candidates.get(new Random().nextInt(candidates.size()));

        // 선정된 멤버 횟수 증가 및 룸에 현재 발표자로 등록
        selected.setPresentationCount(selected.getPresentationCount() + 1);
        room.setCurrentPresenterId(selected.getId());

        return selected;
    }
}