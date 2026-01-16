package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.PostRequestDto;
import com.example.madcamp_2026_winter_MV.dto.PostResponseDto;
import com.example.madcamp_2026_winter_MV.dto.VoteDto;
import com.example.madcamp_2026_winter_MV.entity.*;
import com.example.madcamp_2026_winter_MV.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final VoteRecordRepository voteRecordRepository;

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
                .maxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 0)
                .currentParticipants(dto.getType() == PostType.PARTY ? 1 : 0)
                .build();

        if (dto.getType() == PostType.VOTE && dto.getVoteContents() != null) {
            for (String voteContent : dto.getVoteContents()) {
                VoteOption option = VoteOption.builder()
                        .content(voteContent)
                        .build();
                post.addVoteOption(option);
            }
        }

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
    public List<PostResponseDto> getAllPosts() {
        return postRepository.findAll().stream()
                .map(post -> PostResponseDto.builder()
                        .postId(post.getPostId())
                        .title(post.getTitle())
                        .content(post.getContent())
                        .type(post.getType())
                        .authorNickname(post.getMember().getNickname())
                        .createdAt(post.getCreatedAt())
                        .currentParticipants(post.getCurrentParticipants())
                        .maxParticipants(post.getMaxParticipants())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPostDetail(Long postId, String email) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 투표 참여 여부 확인
        boolean isVoted = voteRecordRepository.existsByMemberMemberIdAndPostPostId(member.getMemberId(), postId);

        // 투표 항목 DTO 변환 (투표 안했으면 count 가림)
        List<VoteDto.VoteResponse> voteOptions = post.getVoteOptions().stream()
                .map(option -> VoteDto.VoteResponse.builder()
                        .optionId(option.getId())
                        .content(option.getContent())
                        .count(isVoted ? option.getCount() : 0) // 투표 전엔 0으로 표시
                        .build())
                .collect(Collectors.toList());

        return PostResponseDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .type(post.getType())
                .authorNickname(post.getMember().getNickname())
                .createdAt(post.getCreatedAt())
                .isVoted(isVoted)
                .voteOptions(voteOptions)
                .currentParticipants(post.getCurrentParticipants())
                .maxParticipants(post.getMaxParticipants())
                .build();
    }

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