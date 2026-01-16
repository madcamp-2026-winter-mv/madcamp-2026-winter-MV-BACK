package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/join")
    public ResponseEntity<String> joinRoom(@RequestParam String inviteCode,
                                           @AuthenticationPrincipal OAuth2User principal) {
        // 1. 구글 로그인 정보에서 이메일을 가져옴
        String email = principal.getAttribute("email");

        // 2. RoomService에서 만든 joinRoomByEmail 메서드를 호출
        roomService.joinRoomByEmail(email, inviteCode);

        return ResponseEntity.ok("방 입장에 성공했습니다.");
    }
}