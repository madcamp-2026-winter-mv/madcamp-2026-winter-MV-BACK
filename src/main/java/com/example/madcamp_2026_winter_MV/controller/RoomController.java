package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Role;
import com.example.madcamp_2026_winter_MV.entity.Room;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final MemberRepository memberRepository;

    // 분반 입장 (초대코드 활용)
    @PostMapping("/join")
    public ResponseEntity<String> joinRoom(@RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        String inviteCode = body.get("inviteCode");
        roomService.joinRoomByEmail(email, inviteCode);
        return ResponseEntity.ok("방 입장에 성공했습니다.");
    }

    // 다음 발표자 선정 (본인 또는 운영진만 가능)
    @PostMapping("/{roomId}/presenter/next")
    public ResponseEntity<?> pickNext(@PathVariable Long roomId,
                                      @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        Member requestMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Room room = requestMember.getRoom();
        // 본인 분반 체크
        if (room == null || !room.getRoomId().equals(roomId)) {
            return ResponseEntity.status(403).body("해당 분반 소속이 아닙니다.");
        }

        boolean isAdmin = requestMember.getRole() == Role.ADMIN || requestMember.getRole() == Role.OWNER;
        boolean isCurrentPresenter = requestMember.getMemberId().equals(room.getCurrentPresenterId());

        if (!isAdmin && !isCurrentPresenter) {
            return ResponseEntity.status(403).body("권한이 없습니다.");
        }

        boolean isHandover = isCurrentPresenter;
        Member nextPresenter = roomService.pickNextPresenter(roomId, isHandover);

        return ResponseEntity.ok(Map.of(
                "message", isHandover ? "다음 발표자는 " + nextPresenter.getNickname() + "님입니다." : "발표자를 재선정했습니다.",
                "nextPresenterNickname", nextPresenter.getNickname()
        ));
    }

    // 출석 시작 (운영진 또는 발표자)
    @PostMapping("/{roomId}/attendance/start")
    public ResponseEntity<?> startAttendance(@PathVariable Long roomId,
                                             @RequestBody Map<String, Integer> body,
                                             @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        Member member = memberRepository.findByEmail(email).orElseThrow();

        Room room = member.getRoom();
        if (room == null || !room.getRoomId().equals(roomId)) {
            return ResponseEntity.status(403).body("본인 분반의 출석만 관리할 수 있습니다.");
        }

        boolean isAdmin = member.getRole() == Role.ADMIN || member.getRole() == Role.OWNER;
        boolean isCurrentPresenter = member.getMemberId().equals(room.getCurrentPresenterId());

        if (!isAdmin && !isCurrentPresenter) {
            return ResponseEntity.status(403).body("출석 권한이 없습니다.");
        }

        int minutes = body.getOrDefault("minutes", 5);
        roomService.startAttendance(roomId, minutes);
        return ResponseEntity.ok(Map.of("message", minutes + "분 동안 출석이 시작되었습니다."));
    }

    // 일반 사용자 출석 제출
    @PostMapping("/attendance/submit")
    public ResponseEntity<?> submitAttendance(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        try {
            roomService.submitAttendance(email);
            return ResponseEntity.ok(Map.of("message", "출석 체크 완료!"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}