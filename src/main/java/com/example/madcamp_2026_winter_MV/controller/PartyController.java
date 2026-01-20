package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.ChatMemberResponseDto;
import com.example.madcamp_2026_winter_MV.dto.ChatMessageDto;
import com.example.madcamp_2026_winter_MV.dto.LeaveOrKickResult;
import com.example.madcamp_2026_winter_MV.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/party")
public class PartyController {

    private static final Logger log = LoggerFactory.getLogger(PartyController.class);
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
            @RequestBody List<Object> rawMemberIds, // List로 직접 받음
            @AuthenticationPrincipal OAuth2User principal) {

        // 1. 전달받은 리스트를 Long 타입으로 변환 (숫자 타입 불일치 방지)
        List<Long> selectedMemberIds = new java.util.ArrayList<>();
        if (rawMemberIds != null) {
            for (Object id : rawMemberIds) {
                selectedMemberIds.add(Long.valueOf(id.toString()));
            }
        }

        int size = selectedMemberIds.size();
        String email = (principal != null) ? (String) principal.getAttribute("email") : null;

        log.info("[채팅방개설] Controller 진입 postId={} selectedMemberIds.size={} selectedMemberIds={} email={}",
                postId, size, selectedMemberIds, (email != null && email.length() > 2) ? email.substring(0, 2) + "***" : email);

        try {
            Long chatRoomId = partyService.confirmAndCreateChat(postId, selectedMemberIds, email);
            log.info("[채팅방개설] Controller 성공 postId={} chatRoomId={}", postId, chatRoomId);
            return ResponseEntity.ok(chatRoomId);
        } catch (Exception ex) {
            log.error("[채팅방개설] Controller 예외 postId={} error={} message={}",
                    postId, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    // 멤버 퇴장/강퇴 시 닉네임 포함 알림 (몰입캠프 시스템 메시지, DB 저장 후 실시간 전송)
    @DeleteMapping("/rooms/{chatRoomId}/members/{memberId}")
    public ResponseEntity<Void> leaveOrKickMember(
            @PathVariable Long chatRoomId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");

        LeaveOrKickResult result = partyService.leaveParty(chatRoomId, memberId, email);
        String content = result.isKicked()
                ? (result.getTargetNickname() + "님이 강퇴당했습니다.")
                : (result.getTargetNickname() + "님이 채팅방을 나갔습니다.");

        // DB에 시스템 메시지 저장 (새로고침 후에도 유지)
        partyService.saveSystemMessage(chatRoomId, "몰입캠프", content);

        // 실시간 퇴장/강퇴 알림 (발언자: 몰입캠프, 프로필: /madcamp_logo.png)
        ChatMessageDto exitMsg = ChatMessageDto.builder()
                .chatRoomId(chatRoomId)
                .senderNickname("몰입캠프")
                .senderProfileImageUrl("/madcamp_logo.png")
                .content(content)
                .timestamp(LocalDateTime.now().toString())
                .build();
        messagingTemplate.convertAndSend("/sub/chat/room/" + chatRoomId, exitMsg);

        return ResponseEntity.ok().build();
    }

    // 멤버 추가 초대 시 닉네임 포함 알림 (몰입캠프 시스템 메시지, DB 저장 후 실시간 전송)
    @PostMapping("/rooms/{chatRoomId}/members/{memberId}")
    public ResponseEntity<Void> addMember(
            @PathVariable Long chatRoomId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");

        String newNickname = partyService.addMemberToParty(chatRoomId, memberId, email);
        String content = newNickname + "님이 들어왔습니다!";

        // DB에 시스템 메시지 저장 (새로고침 후에도 유지)
        partyService.saveSystemMessage(chatRoomId, "몰입캠프", content);

        // 실시간 입장 알림 (발언자: 몰입캠프, 프로필: /madcamp_logo.png)
        ChatMessageDto enterMsg = ChatMessageDto.builder()
                .chatRoomId(chatRoomId)
                .senderNickname("몰입캠프")
                .senderProfileImageUrl("/madcamp_logo.png")
                .content(content)
                .timestamp(LocalDateTime.now().toString())
                .build();
        messagingTemplate.convertAndSend("/sub/chat/room/" + chatRoomId, enterMsg);

        return ResponseEntity.ok().build();
    }

    // 채팅방 멤버 목록 조회 (참가자만 가능, isOwner 포함)
    @GetMapping("/rooms/{chatRoomId}/members")
    public ResponseEntity<List<ChatMemberResponseDto>> getChatRoomMembers(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) return ResponseEntity.status(401).build();

        String email = principal.getAttribute("email");
        List<ChatMemberResponseDto> members = partyService.getChatRoomMembers(chatRoomId, email);
        return ResponseEntity.ok(members);
    }

    // 채팅방 입장 시 읽음 시간 업데이트 (미읽음 카운트 0 처리용)
    @PostMapping("/rooms/{chatRoomId}/read")
    public ResponseEntity<Void> updateReadTime(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String email = principal.getAttribute("email");
        partyService.updateLastReadTime(chatRoomId, email);

        return ResponseEntity.ok().build();
    }

    // 채팅방 삭제 (방장 전용)
    @DeleteMapping("/rooms/{chatRoomId}")
    public ResponseEntity<Void> deleteChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) return ResponseEntity.status(401).build();

        String email = principal.getAttribute("email");
        partyService.deleteChatRoom(chatRoomId, email);

        return ResponseEntity.noContent().build();
    }
}