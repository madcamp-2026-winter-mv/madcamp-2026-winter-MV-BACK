package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.dto.PostResponseDto;
import com.example.madcamp_2026_winter_MV.entity.Comment;
import com.example.madcamp_2026_winter_MV.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}/comments")
    public ResponseEntity<PostResponseDto.CommentResponseDto> createComment(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        String content = body.get("content") != null ? body.get("content").toString() : null;
        boolean anonymous = "true".equals(String.valueOf(body.get("anonymous")));

        Comment comment = commentService.createComment(postId, content, email, anonymous);

        Long cRoomId = (comment.isAnonymous() || comment.getMember().getRoom() == null) ? null : comment.getMember().getRoom().getRoomId();
        String cImg = comment.isAnonymous() || comment.getMember() == null ? null : comment.getMember().getProfileImage();
        PostResponseDto.CommentResponseDto response = PostResponseDto.CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .memberId(comment.getMember().getMemberId())
                .content(comment.getContent())
                .authorNickname(comment.isAnonymous() ? "익명" : comment.getMember().getNickname())
                .createdAt(comment.getCreatedAt())
                .isAnonymous(comment.isAnonymous())
                .roomId(cRoomId)
                .imageUrl(cImg)
                .build();

        return ResponseEntity.ok(response);
    }
    // 댓글 수정
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<PostResponseDto.CommentResponseDto> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        String content = body.get("content");

        Comment comment = commentService.updateComment(commentId, content, email);

        return ResponseEntity.ok(PostResponseDto.CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .authorNickname(comment.isAnonymous() ? "익명" : comment.getMember().getNickname())
                .createdAt(comment.getCreatedAt())
                .isAnonymous(comment.isAnonymous())
                .build());
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        commentService.deleteComment(commentId, email);

        return ResponseEntity.ok("댓글이 삭제되었습니다.");
    }
}