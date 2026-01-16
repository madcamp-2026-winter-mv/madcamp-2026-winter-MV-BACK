package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.PostRequestDto;
import com.example.madcamp_2026_winter_MV.entity.*;
import com.example.madcamp_2026_winter_MV.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public Post createPost(PostRequestDto dto, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .type(dto.getType())
                .member(member)
                .room(room)
                .category(category)
                // 팟모집일 경우 DTO에서 받은 최대 인원 설정 (기본값 0)
                .maxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 0)
                .currentParticipants(dto.getType() == PostType.PARTY ? 1 : 0) // 작성자 본인 포함
                .build();

        return postRepository.save(post);
    }

    @Transactional
    public void joinParty(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (post.getType() != PostType.PARTY) {
            throw new IllegalStateException("팟모집 게시글이 아닙니다.");
        }

        if (post.getCurrentParticipants() >= post.getMaxParticipants()) {
            throw new IllegalStateException("모집 인원이 가득 찼습니다.");
        }

        post.setCurrentParticipants(post.getCurrentParticipants() + 1);
    }

    @Transactional(readOnly = true)
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    // 추가된 기능: 게시글 상세 조회
    @Transactional(readOnly = true)
    public Post getPostDetail(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));
    }

    // 추가된 기능: 댓글 작성
    @Transactional
    public Comment createComment(Long postId, String content, String email) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .post(post)
                .member(member)
                .content(content)
                .build();

        return commentRepository.save(comment);
    }
}