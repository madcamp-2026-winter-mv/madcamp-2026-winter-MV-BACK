package com.example.madcamp_2026_winter_MV.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    OWNER("ROLE_OWNER", "방장"),
    ADMIN("ROLE_ADMIN", "운영진"),
    USER("ROLE_USER", "일반유저");

    private final String key;
    private final String title;
}