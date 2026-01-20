package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.ChatMessageDto;
import com.example.madcamp_2026_winter_MV.dto.ChatRoomResponseDto;
import com.example.madcamp_2026_winter_MV.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final PartyService partyService;
    private final SimpMessagingTemplate messagingTemplate;

    // 1. [HTTP] 내 채팅방 목록 조회
    @GetMapping("/api/chat/rooms")
    public ResponseEntity<List<ChatRoomResponseDto>> getMyRooms(
            @AuthenticationPrincipal OAuth2User principal) {
        String email = (principal != null) ? principal.getAttribute("email") : "test@gmail.com";
        log.info("[채팅방목록] 조회 요청 - email: {}", email);

        List<ChatRoomResponseDto> rooms = partyService.getMyChatRooms(email);
        return ResponseEntity.ok(rooms);
    }

    // 2. [HTTP] 특정 채팅방의 모든 대화 내역 조회
    @GetMapping("/api/chat/rooms/{chatRoomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(@PathVariable Long chatRoomId) {
        log.info("[메시지내역] 조회 요청 - roomId: {}", chatRoomId);

        List<ChatMessageDto> messages = partyService.getChatMessages(chatRoomId);
        return ResponseEntity.ok(messages);
    }

    // 3. [WebSocket] 실시간 메시지 전송 및 저장
    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageDto messageDto) {
        // 메시지 수신 로그 (가장 중요)
        log.info("[웹소켓메시지] 수신 - roomId: {}, sender: {}, content: {}",
                messageDto.getChatRoomId(), messageDto.getSenderNickname(), messageDto.getContent());

        try {
            // DB에 메시지 영구 저장
            partyService.saveMessage(messageDto);

            // 브로드캐스팅 로그
            log.info("[웹소켓메시지] 브로드캐스팅 - target: /sub/chat/room/{}", messageDto.getChatRoomId());
            messagingTemplate.convertAndSend("/sub/chat/room/" + messageDto.getChatRoomId(), messageDto);

        } catch (Exception e) {
            log.error("[웹소켓메시지] 처리 중 에러 발생: {}", e.getMessage());
        }
    }
}