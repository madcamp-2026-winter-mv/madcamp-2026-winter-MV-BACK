package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.entity.Comment;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.repository.CommentRepository;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Transactional
    public Comment createComment(Long postId, String content, String email) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. 댓글 저장
        Comment comment = Comment.builder()
                .post(post)
                .member(member)
                .content(content)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 2. 알림 생성 로직
        Member postAuthor = post.getMember();
        if (postAuthor != null && !postAuthor.getMemberId().equals(member.getMemberId())) {
            if (postAuthor.isAllowAlarm()) {
                notificationService.createNotification(
                        postAuthor,
                        post,
                        "'" + post.getTitle() + "' 게시글에 새 댓글이 달렸습니다."
                );
            }
        }

        return savedComment;
    }
}