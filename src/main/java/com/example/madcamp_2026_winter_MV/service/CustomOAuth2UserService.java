package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Role;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name"); // 구글 프로필 실명
        String picture = (String) attributes.get("picture");

        Member member = saveOrUpdate(email, name, picture);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().name())),
                attributes,
                userNameAttributeName
        );
    }

    private Member saveOrUpdate(String email, String name, String picture) {
        Member member = memberRepository.findByEmail(email)
                .map(entity -> {
                    // 기존 회원이면 실명(realName)과 프로필 이미지를 업데이트
                    entity.setRealName(name);
                    entity.setProfileImage(picture);
                    return entity;
                })
                .orElseGet(() -> {
                    // 신규 회원이면 닉네임은 랜덤, 실명은 구글 이름으로 저장
                    String randomNickname = "몰입하는 " + (int)(Math.random() * 900 + 100);
                    return Member.builder()
                            .email(email)
                            .realName(name) // 구글 실명 저장
                            .nickname(randomNickname)
                            .profileImage(picture)
                            .role(Role.USER)
                            .allowAlarm(true)
                            .presentationCount(0)
                            .build();
                });

        return memberRepository.save(member);
    }
}