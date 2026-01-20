package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.MemberResponseDto;
import com.example.madcamp_2026_winter_MV.dto.ScheduleRequestDto;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Room;
import com.example.madcamp_2026_winter_MV.entity.Role;
import com.example.madcamp_2026_winter_MV.entity.Schedule;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.repository.RoomRepository;
import com.example.madcamp_2026_winter_MV.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final ScheduleRepository scheduleRepository;

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
                        .memberId(member.getMemberId())
                        .realName(member.getRealName()) // 실명 포함
                        .nickname(member.getNickname())
                        .email(member.getEmail())
                        .role(member.getRole() != null ? member.getRole().name() : "USER")
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

        // 1. 출석 가능 시간 및 활성화 여부 체크 (기존 로직)
        if (!room.isAttendanceActive() || room.getAttendanceEndTime() == null ||
                LocalDateTime.now().isAfter(room.getAttendanceEndTime())) {
            room.stopAttendance();
            throw new IllegalStateException("출석 가능 시간이 아닙니다.");
        }

        // 2. 중복 출석 체크 로직 추가
        if (member.getLastAttendanceTime() != null &&
                member.getLastAttendanceTime().toLocalDate().equals(LocalDate.now())) {
            throw new IllegalStateException("이미 출석 처리가 완료되었습니다.");
        }

        // 3. 출석 처리 및 시간 기록
        member.setAttendanceCount(member.getAttendanceCount() + 1);
        member.setLastAttendanceTime(LocalDateTime.now()); // 현재 시각 기록
    }

    @Transactional
    public void stopAttendance(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        room.stopAttendance();
    }

    //운영진이 날짜와 시간을 지정하여 일정을 등록하는 로직
    @Transactional
    public void addSchedule(Long roomId, ScheduleRequestDto dto) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        Schedule schedule = Schedule.builder()
                .room(room)
                .title(dto.getTitle())
                .content(dto.getContent())
                .startTime(dto.getStartTime())
                .isImportant(dto.isImportant())
                .build();

        scheduleRepository.save(schedule);
    }

    // 분반 멤버들이 일정을 조회하는 로직 (시간순 정렬)
    public List<Schedule> getSchedules(Long roomId) {
        return scheduleRepository.findByRoom_RoomIdOrderByStartTimeAsc(roomId);
    }

    // 멤버 강퇴 로직-소속 분반 아이디(Room)를 제거
    @Transactional
    public void kickMember(Long roomId, Long targetMemberId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("분반을 찾을 수 없습니다."));

        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new IllegalArgumentException("대상 멤버를 찾을 수 없습니다."));

        // 멤버가 해당 분반 소속인지 확인
        if (member.getRoom() == null || !member.getRoom().getRoomId().equals(roomId)) {
            throw new IllegalArgumentException("해당 분반에 소속된 멤버가 아닙니다.");
        }

        // 1. Room 엔티티의 멤버 리스트에서 제거
        room.getMembers().remove(member);

        // 2. Member 엔티티의 Room 참조를 null로 설정
        member.setRoom(null);

        // 3. 역할은 기존 USER(일반 유저) 상태를 유지
        member.setRole(Role.USER);
    }
}