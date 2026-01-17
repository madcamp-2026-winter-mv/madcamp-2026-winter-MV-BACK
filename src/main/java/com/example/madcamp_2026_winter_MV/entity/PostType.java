package com.example.madcamp_2026_winter_MV.entity;

public enum PostType {
    // 게시판 타입
    NORMAL, VOTE, PARTY,

    // 대시보드 전용 추가 타입
    ATTENDANCE,   // 실시간 출석 현황용
    PRESENTER,    // 오늘의 발표자용
    SCHEDULE      // 오늘의 일정용
}