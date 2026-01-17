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
}