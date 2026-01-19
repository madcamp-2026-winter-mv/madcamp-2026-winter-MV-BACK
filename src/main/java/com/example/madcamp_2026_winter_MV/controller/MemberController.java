package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.MemberResponseDto;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.service.MemberService;
import com.example.madcamp_2026_winter_MV.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final RoomService roomService; // 2. RoomService 주입 추가 (이게 없어서 빨간줄이 뜬 겁니다)

    // 1. 내 정보 및 통계 조회
    @GetMapping("/me")
    public ResponseEntity<MemberResponseDto> getMyInfo(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        return ResponseEntity.ok(memberService.getMyInfo(email));
    }

    // 2. 닉네임 중복 체크
    @GetMapping("/check/nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean isDuplicate = memberRepository.existsByNickname(nickname);
        return ResponseEntity.ok(isDuplicate);
    }

    // 3. 닉네임 수정
    @PatchMapping("/me/nickname")
    public ResponseEntity<String> updateNickname(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        String newNickname = body.get("nickname");

        memberService.updateNickname(email, newNickname);
        return ResponseEntity.ok("닉네임이 성공적으로 수정되었습니다.");
    }

    // 4. 알림 설정 토글
    @PatchMapping("/me/alarm")
    public ResponseEntity<Boolean> toggleAlarm(
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        boolean allowAlarm = body.get("allowAlarm");

        boolean updatedStatus = memberService.updateAlarmStatus(email, allowAlarm);
        return ResponseEntity.ok(updatedStatus);
    }

    // 5. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok("로그아웃되었습니다.");
    }

    // 6. 분반 탈퇴
    @DeleteMapping("/me/room")
    public ResponseEntity<String> leaveRoom(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        memberService.leaveRoom(email);
        return ResponseEntity.ok("분반에서 탈퇴하였습니다.");
    }

    // 7. 분반 코드(8자리)로 분반 입장
    @PostMapping("/me/room/join")
    public ResponseEntity<String> joinRoom(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        String inviteCode = body.get("inviteCode");

        roomService.joinRoomByEmail(email, inviteCode);

        return ResponseEntity.ok("분반에 성공적으로 입장하였습니다.");
    }
}