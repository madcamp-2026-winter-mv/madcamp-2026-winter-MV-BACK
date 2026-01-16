package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Role;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 구글 로그인인지 확인하는 ID (google)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // OAuth2 로그인 진행 시 키가 되는 필드값 (구글은 'sub'이 기본)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 구글로부터 받은 유저 속성들
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        // 유저 저장 및 업데이트 로직
        Member member = saveOrUpdate(email, name, picture);

        return new DefaultOAuth2User(
                Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority(member.getRole().getKey())),
                attributes,
                userNameAttributeName
        );
    }

    private Member saveOrUpdate(String email, String name, String picture) {
        return memberRepository.findByEmail(email)
                .map(entity -> entity.update(name, picture)) // 이미 있으면 정보 갱신
                .orElseGet(() -> {
                    // 신규 회원이면 "몰입하는 [랜덤숫자]"로 닉네임 자동 생성
                    String randomNickname = "몰입하는 " + (int)(Math.random() * 900 + 100);
                    return memberRepository.save(Member.builder()
                            .email(email)
                            .realName(name)
                            .nickname(randomNickname)
                            .profileImage(picture)
                            .role(Role.USER) // 초기 권한은 일반 유저
                            .build());
                });
    }
}