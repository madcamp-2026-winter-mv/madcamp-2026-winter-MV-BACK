package com.example.madcamp_2026_winter_MV.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String realName;

    @Column(nullable = false)
    private String nickname;

    @Column(columnDefinition = "TEXT")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    private int presentationCount = 0;

    @Builder.Default
    private boolean allowAlarm = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    // OAuth2 로그인 시 유저 정보 업데이트를 위한 메서드
    public Member update(String realName, String profileImage) {
        this.realName = realName;
        this.profileImage = profileImage;
        return this;
    }
}