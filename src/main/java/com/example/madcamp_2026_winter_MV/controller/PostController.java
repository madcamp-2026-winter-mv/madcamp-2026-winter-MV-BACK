package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.PostRequestDto;
import com.example.madcamp_2026_winter_MV.dto.PostResponseDto;
import com.example.madcamp_2026_winter_MV.entity.Comment;
import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.service.PostService;
import com.example.madcamp_2026_winter_MV.service.CommentService; // 추가
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
    private final CommentService commentService; // CommentService 주입 추가

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

    @PostMapping("/{postId}/comments")
    public ResponseEntity<PostResponseDto.CommentResponseDto> createComment(@PathVariable Long postId,
                                                                            @RequestBody Map<String, String> body,
                                                                            @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        String content = body.get("content");

        // PostService 대신 CommentService를 호출하여 댓글 생성 및 알림 로직 실행
        Comment comment = commentService.createComment(postId, content, email);

        PostResponseDto.CommentResponseDto response = PostResponseDto.CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .authorNickname(comment.getMember().getNickname())
                .createdAt(comment.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<PostResponseDto>> getMyPosts(@AuthenticationPrincipal OAuth2User principal) {
        String email = (principal != null) ? principal.getAttribute("email") : "test@gmail.com";
        return ResponseEntity.ok(postService.getMyPosts(email));
    }

    @GetMapping("/me/comments")
    public ResponseEntity<List<PostResponseDto>> getPostsICommented(@AuthenticationPrincipal OAuth2User principal) {
        String email = (principal != null) ? principal.getAttribute("email") : "test@gmail.com";
        return ResponseEntity.ok(postService.getPostsICommented(email));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostResponseDto> updatePost(
            @PathVariable Long postId,
            @RequestBody PostRequestDto dto,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        postService.updatePost(postId, dto, email);
        return ResponseEntity.ok(postService.getPostDetail(postId, email));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        postService.deletePost(postId, email);
        return ResponseEntity.ok("게시글이 삭제되었습니다.");
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<PostResponseDto>> getPostsByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(postService.getPostsByRoom(roomId));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<PostResponseDto>> getPostsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(postService.getPostsByCategory(categoryId));
    }

    @GetMapping("/room/{roomId}/dashboard")
    public ResponseEntity<Map<String, Object>> getRoomDashboard(@PathVariable Long roomId) {
        return ResponseEntity.ok(postService.getRoomDashboardData(roomId));
    }

    @GetMapping("/common/hot3")
    public ResponseEntity<List<PostResponseDto>> getHot3Posts() {
        return ResponseEntity.ok(postService.getHot3Posts());
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<String> toggleLike(@PathVariable Long postId,
                                             @AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        postService.toggleLike(postId, email);
        return ResponseEntity.ok("좋아요 상태가 변경되었습니다.");
    }
}