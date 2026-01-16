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

    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(@RequestBody PostRequestDto dto,
                                                      @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        Post savedPost = postService.createPost(dto, email);
        return ResponseEntity.ok(postService.getPostDetail(savedPost.getPostId(), email));
    }

    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @PostMapping("/{postId}/join")
    public ResponseEntity<String> joinParty(@PathVariable Long postId) {
        postService.joinParty(postId);
        return ResponseEntity.ok("팟 참여에 성공했습니다.");
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPostDetail(@PathVariable Long postId,
                                                         @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        PostResponseDto postDetail = postService.getPostDetail(postId, email);
        return ResponseEntity.ok(postDetail);
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    public ResponseEntity<PostResponseDto.CommentResponseDto> createComment(@PathVariable Long postId,
                                                                            @RequestBody Map<String, String> body,
                                                                            @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        String content = body.get("content");
        Comment comment = postService.createComment(postId, content, email);

        // 엔티티를 DTO로 변환하여 반환
        PostResponseDto.CommentResponseDto response = PostResponseDto.CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .authorNickname(comment.getMember().getNickname())
                .createdAt(comment.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }
}