package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.NotificationResponseDto;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    // 1. 실시간 알림 구독 (SSE)
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }
        String email = principal.getAttribute("email");
        return notificationService.subscribe(email);
    }

    // 2. 내 알림 전체 목록 조회
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getMyNotifications(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String email = principal.getAttribute("email");
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return ResponseEntity.ok(notificationService.getNotifications(member));
    }

    //  3. 알림 클릭 (읽음 처리 및 이동 URL 반환)
    @PostMapping("/{notificationId}/click")
    public ResponseEntity<String> clickNotification(@PathVariable Long notificationId) {
        String redirectUrl = notificationService.clickNotification(notificationId);
        return ResponseEntity.ok(redirectUrl);
    }

    // 4. 알림 삭제 (X 버튼 클릭 시)
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }
}