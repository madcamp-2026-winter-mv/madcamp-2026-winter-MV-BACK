package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.PostRequestDto;
import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 작성
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody PostRequestDto dto,
                                           @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        Post savedPost = postService.createPost(dto, email);
        return ResponseEntity.ok(savedPost);
    }

    // 게시글 전체 목록 조회
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // 팟 참여하기 (인원수 증가)
    @PostMapping("/{postId}/join")
    public ResponseEntity<String> joinParty(@PathVariable Long postId) {
        postService.joinParty(postId);
        return ResponseEntity.ok("팟 참여에 성공했습니다.");
    }
}