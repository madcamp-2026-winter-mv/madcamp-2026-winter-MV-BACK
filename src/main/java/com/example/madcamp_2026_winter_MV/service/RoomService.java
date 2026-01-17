package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Room;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

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

    // 발표자 랜덤 선정 로직
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
        room.updatePresenter(selected.getMemberId());

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

    // 운영진용 초대 코드 생성
    @Transactional
    public String generateInviteCode(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 분반이 존재하지 않습니다."));

        // 8자리 무작위 초대 코드 생성
        String newCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        room.setInviteCode(newCode);

        return newCode;
    }

    // 모든 분반 목록 조회
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    // 출석 시작 로직
    @Transactional
    public void startAttendance(Long roomId, int minutes) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        room.incrementTotalSessions(); // 전체 세션 수 증가
        room.startAttendance(minutes); // 마감 시간 설정 및 활성화
    }

    // 출석 제출 로직
    @Transactional
    public void submitAttendance(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Room room = member.getRoom();
        if (room == null) throw new IllegalStateException("소속된 분반이 없습니다.");

        // 출석 마감 시간 및 활성화 여부 체크
        if (!room.isAttendanceActive() || room.getAttendanceEndTime() == null ||
                LocalDateTime.now().isAfter(room.getAttendanceEndTime())) {
            room.stopAttendance(); // 시간이 지났으면 상태 업데이트
            throw new IllegalStateException("출석 가능 시간이 아닙니다.");
        }

        // 출석 횟수 증가
        member.setAttendanceCount(member.getAttendanceCount() + 1);
    }
    @Transactional
    public void stopAttendance(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        room.stopAttendance();
    }
}