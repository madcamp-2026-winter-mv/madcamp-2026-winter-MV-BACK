package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.entity.Comment;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.repository.CommentRepository;
import com.example.madcamp_2026_winter_MV.repository.MemberRepository;
import com.example.madcamp_2026_winter_MV.repository.PostRepository;
import com.example.madcamp_2026_winter_MV.repository.PostTempParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostTempParticipantRepository postTempParticipantRepository;
    private final NotificationService notificationService;

    @Transactional
    public Comment createComment(Long postId, String content, String email, boolean anonymous) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용을 입력해 주세요.");
        }

        // 1. 댓글 저장
        Comment comment = Comment.builder()
                .post(post)
                .member(member)
                .content(content.trim())
                .isAnonymous(anonymous)
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
    @Transactional
    public Comment updateComment(Long commentId, String content, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 작성자 검증
        if (!comment.getMember().getEmail().equals(email)) {
            throw new IllegalStateException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("수정할 내용을 입력해 주세요.");
        }

        comment.setContent(content.trim());
        return comment;
    }

    @Transactional
    public void deleteComment(Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 작성자 검증
        if (!comment.getMember().getEmail().equals(email)) {
            throw new IllegalStateException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        // 팟 모집 참가자로 선택된 댓글 작성자면 임시 참가자 목록에서 제거
        Long postId = comment.getPost().getPostId();
        Long memberId = comment.getMember().getMemberId();
        postTempParticipantRepository.findByPost_PostIdAndMember_MemberId(postId, memberId)
                .ifPresent(postTempParticipantRepository::delete);

        commentRepository.delete(comment);
    }


}