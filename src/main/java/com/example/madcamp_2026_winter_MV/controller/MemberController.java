package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.MemberResponseDto;
import com.example.madcamp_2026_winter_MV.service.MemberService;
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

    // 1. 내 정보 및 통계 조회
    @GetMapping("/me")
    public ResponseEntity<MemberResponseDto> getMyInfo(@AuthenticationPrincipal OAuth2User principal) {
        String email = (principal != null) ? principal.getAttribute("email") : "test@gmail.com";
        return ResponseEntity.ok(memberService.getMyInfo(email));
    }

    // 2. 닉네임 수정
    @PatchMapping("/me/nickname")
    public ResponseEntity<String> updateNickname(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = (principal != null) ? principal.getAttribute("email") : "test@gmail.com";
        String newNickname = body.get("nickname");

        memberService.updateNickname(email, newNickname);
        return ResponseEntity.ok("닉네임이 성공적으로 수정되었습니다.");
    }

    // 3. 알림 설정 토글
    @PatchMapping("/me/alarm")
    public ResponseEntity<Boolean> toggleAlarm(
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = (principal != null) ? principal.getAttribute("email") : "test@gmail.com";
        boolean allowAlarm = body.get("allowAlarm");

        boolean updatedStatus = memberService.updateAlarmStatus(email, allowAlarm);
        return ResponseEntity.ok(updatedStatus);
    }

    // 4. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok("로그아웃되었습니다.");
    }

    // 5. 분반 탈퇴
    @DeleteMapping("/me/room")
    public ResponseEntity<String> leaveRoom(@AuthenticationPrincipal OAuth2User principal) {
        String email = (principal != null) ? principal.getAttribute("email") : "test@gmail.com";
        memberService.leaveRoom(email);
        return ResponseEntity.ok("분반에서 탈퇴하였습니다.");
    }
}