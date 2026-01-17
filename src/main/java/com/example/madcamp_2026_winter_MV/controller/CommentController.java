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
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        String content = body.get("content");

        Comment comment = commentService.createComment(postId, content, email);

        PostResponseDto.CommentResponseDto response = PostResponseDto.CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .authorNickname(comment.getMember().getNickname())
                .createdAt(comment.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }
}