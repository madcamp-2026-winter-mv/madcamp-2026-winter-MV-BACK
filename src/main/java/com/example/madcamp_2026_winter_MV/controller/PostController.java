package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.PostRequestDto;
import com.example.madcamp_2026_winter_MV.dto.PostResponseDto;
import com.example.madcamp_2026_winter_MV.entity.Comment;
import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 작성 (반환 시에도 DTO를 사용하여 프록시 에러 방지)
    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(@RequestBody PostRequestDto dto,
                                                      @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        Post savedPost = postService.createPost(dto, email);

        // 생성 후 바로 상세 정보를 담아 응답 (필요 시 getPostDetail 호출 로직으로 대체 가능)
        return ResponseEntity.ok(postService.getPostDetail(savedPost.getPostId(), email));
    }

    // 게시글 전체 목록 조회 (PostResponseDto 리스트 반환)
    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // 팟 참여하기 (인원수 증가)
    @PostMapping("/{postId}/join")
    public ResponseEntity<String> joinParty(@PathVariable Long postId) {
        postService.joinParty(postId);
        return ResponseEntity.ok("팟 참여에 성공했습니다.");
    }

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPostDetail(@PathVariable Long postId,
                                                         @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        PostResponseDto postDetail = postService.getPostDetail(postId, email);
        return ResponseEntity.ok(postDetail);
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> createComment(@PathVariable Long postId,
                                                 @RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        String content = body.get("content");
        Comment comment = postService.createComment(postId, content, email);
        return ResponseEntity.ok(comment);
    }
}