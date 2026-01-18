package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/party")
public class PartyController {

    private final PartyService partyService;

    // 1. 팟 모집 확정 및 채팅방 생성 (몰입캠프 참여자만 가능)
    @PostMapping("/{postId}/confirm")
    public ResponseEntity<Long> confirmParty(
            @PathVariable Long postId,
            @RequestBody List<Long> selectedMemberIds,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        Long chatRoomId = partyService.confirmAndCreateChat(postId, selectedMemberIds, email);
        return ResponseEntity.ok(chatRoomId);
    }

    // 2. 멤버 강퇴 또는 스스로 나가기
    @DeleteMapping("/rooms/{chatRoomId}/members/{memberId}")
    public ResponseEntity<Void> leaveOrKickMember(
            @PathVariable Long chatRoomId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        partyService.leaveParty(chatRoomId, memberId, email);
        return ResponseEntity.ok().build();
    }

    // 3. 빈자리에 멤버 추가 초대
    @PostMapping("/rooms/{chatRoomId}/members/{memberId}")
    public ResponseEntity<Void> addMember(
            @PathVariable Long chatRoomId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        partyService.addMemberToParty(chatRoomId, memberId, email);
        return ResponseEntity.ok().build();
    }
}