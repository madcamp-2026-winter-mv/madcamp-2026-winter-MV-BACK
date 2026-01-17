package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.MemberResponseDto;
import com.example.madcamp_2026_winter_MV.dto.ScheduleRequestDto;
import com.example.madcamp_2026_winter_MV.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin") // 모든 경로는 /api/admin으로 시작
public class AdminController {

    private final RoomService roomService;

    // 1. 실명 기반 전체 출석부 조회 (운영진/발표자 전용)
    @GetMapping("/rooms/{roomId}/attendance")
    public ResponseEntity<List<MemberResponseDto>> getRealNameAttendance(
            @PathVariable Long roomId,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        return ResponseEntity.ok(roomService.getAttendanceList(roomId, email));
    }

    // 2. 새로운 초대 코드 생성
    @PostMapping("/rooms/{roomId}/invite-code")
    public ResponseEntity<String> refreshInviteCode(
            @PathVariable Long roomId,
            @AuthenticationPrincipal OAuth2User principal) {

        // 권한 체크 로직은 Service에 포함되어 있거나 SecurityConfig에서 설정 가능
        String newCode = roomService.generateInviteCode(roomId);
        return ResponseEntity.ok(newCode);
    }

    // 3. 출석 시작 (시간 설정)
    @PostMapping("/rooms/{roomId}/attendance/start")
    public ResponseEntity<String> startAttendance(
            @PathVariable Long roomId,
            @RequestParam int minutes) {

        roomService.startAttendance(roomId, minutes);
        return ResponseEntity.ok(minutes + "분 동안 출석이 시작되었습니다.");
    }

    // 4. 발표자 랜덤 선정
    @PostMapping("/rooms/{roomId}/presenter/pick")
    public ResponseEntity<MemberResponseDto> pickPresenter(@PathVariable Long roomId) {
        return ResponseEntity.ok(MemberResponseDto.from(roomService.pickPresenter(roomId)));
    }

    // 5. 오늘의 일정 추가
    @PostMapping("/rooms/{roomId}/schedules")
    public ResponseEntity<String> addSchedule(
            @PathVariable Long roomId,
            @RequestBody ScheduleRequestDto dto,
            @AuthenticationPrincipal OAuth2User principal) {

        roomService.addSchedule(roomId, dto);
        return ResponseEntity.ok("오늘의 일정이 등록되었습니다.");
}
    // 6. 멤버 강퇴 기능
    @DeleteMapping("/rooms/{roomId}/members/{targetMemberId}")
    public ResponseEntity<String> kickMember(
            @PathVariable Long roomId,
            @PathVariable Long targetMemberId) {
        roomService.kickMember(roomId, targetMemberId);
        return ResponseEntity.ok("해당 멤버가 분반에서 제외되었습니다.");
    }
}