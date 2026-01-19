package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberRepository memberRepository;

    @GetMapping("/me")
    public Map<String, Object> getUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Map.of("error", "로그인된 사용자가 없습니다.");
        }

        String email = principal.getAttribute("email");

        // DB에서 해당 유저 찾기
        return memberRepository.findByEmail(email)
                .map(member -> Map.<String, Object>of(
                        "email", member.getEmail(),
                        "nickname", member.getNickname(),
                        "realName", member.getRealName() != null ? member.getRealName() : "",
                        "profileImage", member.getProfileImage() != null ? member.getProfileImage() : "",
                        "role", member.getRole().name()
                ))
                .orElse(Map.of("error", "DB에 사용자 정보가 없습니다."));
    }
}