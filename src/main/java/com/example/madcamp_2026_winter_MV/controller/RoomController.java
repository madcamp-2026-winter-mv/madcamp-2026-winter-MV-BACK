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

    @PostMapping("/join")
    public ResponseEntity<String> joinRoom(@RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        String inviteCode = body.get("inviteCode");
        roomService.joinRoomByEmail(email, inviteCode);
        return ResponseEntity.ok("방 입장에 성공했습니다.");
    }

    // 다음 발표자 선정 API (오늘 발표자 본인 또는 운영진만 가능)
    @PostMapping("/{roomId}/presenter/next")
    public ResponseEntity<?> pickNext(@PathVariable Long roomId,
                                      @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        Member requestMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Room room = requestMember.getRoom();
        if (room == null || !room.getRoomId().equals(roomId)) {
            return ResponseEntity.status(403).body("해당 분반 소속이 아닙니다.");
        }

        boolean isAdmin = requestMember.getRole() == Role.ADMIN || requestMember.getRole() == Role.OWNER;
        boolean isCurrentPresenter = requestMember.getMemberId().equals(room.getCurrentPresenterId());

        if (!isAdmin && !isCurrentPresenter) {
            return ResponseEntity.status(403).body("다음 발표자를 뽑을 권한이 없습니다.");
        }

        // 발표자가 다음사람 지정 , 운영진이 요청하면 재선정
        boolean isHandover = isCurrentPresenter;

        Member nextPresenter = roomService.pickNextPresenter(roomId, isHandover);

        String message = isHandover ?
                "다음 발표자는 " + nextPresenter.getNickname() + "님입니다." :
                "발표자를 재선정했습니다. 새로운 발표자는 " + nextPresenter.getNickname() + "님입니다.";

        return ResponseEntity.ok(Map.of(
                "message", message,
                "nextPresenterNickname", nextPresenter.getNickname()
        ));
    }

    // 운영진용 초대 코드 생성 API
    @PostMapping("/{roomId}/invite-code")
    public ResponseEntity<?> generateInviteCode(@PathVariable Long roomId,
                                                @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        Member requestMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 운영진 권한 체크
        if (requestMember.getRole() != Role.ADMIN && requestMember.getRole() != Role.OWNER) {
            return ResponseEntity.status(403).body("초대 코드를 생성할 권한이 없습니다.");
        }

        String newCode = roomService.generateInviteCode(roomId);
        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "inviteCode", newCode,
                "message", "새로운 초대 코드가 생성되었습니다."
        ));
    }

    // 모든 분반 정보 조회 (운영진용)
    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms(@AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    // 출석 시작 API (운영진 또는 오늘의 발표자만 가능)
    @PostMapping("/{roomId}/attendance/start")
    public ResponseEntity<?> startAttendance(@PathVariable Long roomId,
                                             @RequestBody Map<String, Integer> body,
                                             @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        Member member = memberRepository.findByEmail(email).orElseThrow();

        // 소속 확인
        Room room = member.getRoom();
        if (room == null || !room.getRoomId().equals(roomId)) {
            return ResponseEntity.status(403).body("해당 분반에 대한 권한이 없습니다.");
        }

        boolean isAdmin = member.getRole() == Role.ADMIN || member.getRole() == Role.OWNER;
        boolean isCurrentPresenter = member.getMemberId().equals(room.getCurrentPresenterId());

        if (!isAdmin && !isCurrentPresenter) {
            return ResponseEntity.status(403).body("출석을 시작할 권한이 없습니다.");
        }

        int minutes = body.getOrDefault("minutes", 5); // 기본 5분
        roomService.startAttendance(roomId, minutes);

        return ResponseEntity.ok(Map.of("message", minutes + "분 동안 출석이 시작되었습니다."));
    }

    // 출석 종료 API (운영진 또는 오늘의 발표자만 가능)
    @PostMapping("/{roomId}/attendance/stop")
    public ResponseEntity<?> stopAttendance(@PathVariable Long roomId,
                                            @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        Member member = memberRepository.findByEmail(email).orElseThrow();

        // 소속 및 권한 확인
        Room room = member.getRoom();
        if (room == null || !room.getRoomId().equals(roomId)) {
            return ResponseEntity.status(403).body("해당 분반에 대한 권한이 없습니다.");
        }

        boolean isAdmin = member.getRole() == Role.ADMIN || member.getRole() == Role.OWNER;
        boolean isCurrentPresenter = member.getMemberId().equals(room.getCurrentPresenterId());

        if (!isAdmin && !isCurrentPresenter) {
            return ResponseEntity.status(403).body("출석을 종료할 권한이 없습니다.");
        }

        roomService.stopAttendance(roomId);

        return ResponseEntity.ok(Map.of("message", "출석이 강제 종료되었습니다."));
    }

    // 일반 사용자용 출석 제출 API
    @PostMapping("/attendance/submit")
    public ResponseEntity<?> submitAttendance(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        try {
            roomService.submitAttendance(email);
            return ResponseEntity.ok(Map.of("message", "출석 체크가 완료되었습니다!"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}