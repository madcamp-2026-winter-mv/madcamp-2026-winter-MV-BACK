package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHomeDashboard(@RequestParam Long roomId) {
        Map<String, Object> homeData = new HashMap<>();

        // 1. 출석, 일정, 발표자 정보 (PostService의 기존 로직 활용)
        homeData.putAll(postService.getRoomDashboardData(roomId));

        // 2. 핫게 3건 (좋아요 순 상위 3건)
        homeData.put("hotPosts", postService.getHot3Posts());

        return ResponseEntity.ok(homeData);
    }
}