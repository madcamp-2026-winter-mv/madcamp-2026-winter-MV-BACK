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

        PostResponseDto.CommentResponseDto response = PostResponseDto.CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .authorNickname(comment.getMember().getNickname())
                .createdAt(comment.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }


    // 1. 내가 작성한 글 목록 조회
    @GetMapping("/me")
    public ResponseEntity<List<PostResponseDto>> getMyPosts(@AuthenticationPrincipal OAuth2User principal) {
        String email = (principal != null) ? principal.getAttribute("email") : "test@gmail.com";
        return ResponseEntity.ok(postService.getMyPosts(email));
    }

    // 2. 내가 댓글을 단 글 목록 조회
    @GetMapping("/me/comments")
    public ResponseEntity<List<PostResponseDto>> getPostsICommented(@AuthenticationPrincipal OAuth2User principal) {
        String email = (principal != null) ? principal.getAttribute("email") : "test@gmail.com";
        return ResponseEntity.ok(postService.getPostsICommented(email));
    }


    // 3. 게시글 수정 (PATCH /api/posts/{postId})
    @PatchMapping("/{postId}")
    public ResponseEntity<PostResponseDto> updatePost(
            @PathVariable Long postId,
            @RequestBody PostRequestDto dto,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        postService.updatePost(postId, dto, email);
        return ResponseEntity.ok(postService.getPostDetail(postId, email));
    }

    // 4. 게시글 삭제 (DELETE /api/posts/{postId})
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        postService.deletePost(postId, email);
        return ResponseEntity.ok("게시글이 삭제되었습니다.");
    }

    // 5. 우리 반 전용 글 목록 조회
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<PostResponseDto>> getPostsByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(postService.getPostsByRoom(roomId));
    }

    // 6.카테고리별 공통 글 목록 조회
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<PostResponseDto>> getPostsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(postService.getPostsByCategory(categoryId));
    }

}