package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.MemberResponseDto;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Room;
import com.example.madcamp_2026_winter_MV.entity.Role; // Role enum 가정
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

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

    // 운영진 혹은 발표자가 실명으로 출석 현황을 조회하는 로직
    public List<MemberResponseDto> getAttendanceList(Long roomId, String requesterEmail) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        Member requester = memberRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("요청자를 찾을 수 없습니다."));

        // 권한 체크: 운영진(OWNER, ADMIN)이거나 현재 분반의 발표자(currentPresenterId)인지 확인
        boolean isAuthorized = requester.getRole() == Role.OWNER || requester.getRole() == Role.ADMIN
                || requester.getMemberId().equals(room.getCurrentPresenterId());

        if (!isAuthorized) {
            throw new IllegalStateException("출석 명단을 조회할 권한이 없습니다.");
        }

        return room.getMembers().stream()
                .map(member -> MemberResponseDto.builder()
                        .realName(member.getRealName()) // 실명 포함
                        .nickname(member.getNickname())
                        .email(member.getEmail())
                        .attendanceRate(room.getTotalSessionCount() == 0 ? 0 :
                                (double) member.getAttendanceCount() / room.getTotalSessionCount() * 100)
                        .presentationCount(member.getPresentationCount())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public Member pickPresenter(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        List<Member> members = room.getMembers();
        if (members.isEmpty()) {
            throw new IllegalStateException("분반에 멤버가 없습니다.");
        }

        int minCount = members.stream()
                .mapToInt(Member::getPresentationCount)
                .min().orElse(0);

        List<Member> candidates = members.stream()
                .filter(m -> m.getPresentationCount() == minCount)
                .toList();

        Member selected = candidates.get(new Random().nextInt(candidates.size()));

        selected.setPresentationCount(selected.getPresentationCount() + 1);
        room.updatePresenter(selected.getMemberId());

        return selected;
    }

    @Transactional
    public Member pickNextPresenter(Long roomId, boolean isHandover) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        if (!isHandover) {
            if (room.getCurrentPresenterId() != null) {
                memberRepository.findById(room.getCurrentPresenterId()).ifPresent(m -> {
                    if (m.getPresentationCount() > 0) m.setPresentationCount(m.getPresentationCount() - 1);
                });
            }
        }
        return pickPresenter(roomId);
    }

    @Transactional
    public String generateInviteCode(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 분반이 존재하지 않습니다."));

        String newCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        room.setInviteCode(newCode);

        return newCode;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Transactional
    public void startAttendance(Long roomId, int minutes) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        room.incrementTotalSessions();
        room.startAttendance(minutes);
    }

    @Transactional
    public void submitAttendance(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Room room = member.getRoom();
        if (room == null) throw new IllegalStateException("소속된 분반이 없습니다.");

        if (!room.isAttendanceActive() || room.getAttendanceEndTime() == null ||
                LocalDateTime.now().isAfter(room.getAttendanceEndTime())) {
            room.stopAttendance();
            throw new IllegalStateException("출석 가능 시간이 아닙니다.");
        }

        member.setAttendanceCount(member.getAttendanceCount() + 1);
    }

    @Transactional
    public void stopAttendance(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        room.stopAttendance();
    }
}