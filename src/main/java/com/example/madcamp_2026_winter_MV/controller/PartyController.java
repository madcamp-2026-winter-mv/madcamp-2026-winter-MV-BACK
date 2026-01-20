package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.ChatMessageDto;
import com.example.madcamp_2026_winter_MV.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/party")
public class PartyController {

    private final PartyService partyService;
    private final SimpMessagingTemplate messagingTemplate;
    /** 댓글 작성자를 임시 참가자로 토글. body: { "memberId": number }. 중복 미허용. */
    @PostMapping("/{postId}/temp-participants/toggle")
    public ResponseEntity<List<Long>> toggleTempParticipant(
            @PathVariable Long postId,
            @RequestBody java.util.Map<String, Object> body,
            @AuthenticationPrincipal OAuth2User principal) {
        Object m = body.get("memberId");
        if (m == null) throw new IllegalArgumentException("memberId가 필요합니다.");
        Long memberId = m instanceof Number ? ((Number) m).longValue() : Long.parseLong(m.toString());
        String email = principal.getAttribute("email");
        return ResponseEntity.ok(partyService.toggleTempParticipant(postId, memberId, email));
    }

    // 팟 확정 및 채팅방 생성
    @PostMapping("/{postId}/confirm")
    public ResponseEntity<Long> confirmParty(
            @PathVariable Long postId,
            @RequestBody List<Long> selectedMemberIds,
            @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        return ResponseEntity.ok(partyService.confirmAndCreateChat(postId, selectedMemberIds, email));
    }

    // 멤버 퇴장/강퇴 시 닉네임 포함 알림
    @DeleteMapping("/rooms/{chatRoomId}/members/{memberId}")
    public ResponseEntity<Void> leaveOrKickMember(
            @PathVariable Long chatRoomId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");

        // 서비스에서 닉네임 받아오기
        String targetNickname = partyService.leaveParty(chatRoomId, memberId, email);

        // 실시간 퇴장 알림
        ChatMessageDto exitMsg = ChatMessageDto.builder()
                .chatRoomId(chatRoomId)
                .senderNickname("시스템")
                .content(targetNickname + "님이 채팅방을 나갔습니다.")
                .build();
        messagingTemplate.convertAndSend("/sub/chat/room/" + chatRoomId, exitMsg);

        return ResponseEntity.ok().build();
    }

    // 멤버 추가 초대 시 닉네임 포함 알림
    @PostMapping("/rooms/{chatRoomId}/members/{memberId}")
    public ResponseEntity<Void> addMember(
            @PathVariable Long chatRoomId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");

        // 서비스에서 닉네임 받아오기
        String newNickname = partyService.addMemberToParty(chatRoomId, memberId, email);

        // 실시간 입장 알림
        ChatMessageDto enterMsg = ChatMessageDto.builder()
                .chatRoomId(chatRoomId)
                .senderNickname("시스템")
                .content(newNickname + "님이 들어왔습니다!")
                .build();
        messagingTemplate.convertAndSend("/sub/chat/room/" + chatRoomId, enterMsg);

        return ResponseEntity.ok().build();
    }
}