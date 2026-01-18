package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.PostRequestDto;
import com.example.madcamp_2026_winter_MV.dto.PostResponseDto;
import com.example.madcamp_2026_winter_MV.dto.VoteDto;
import com.example.madcamp_2026_winter_MV.entity.*;
import com.example.madcamp_2026_winter_MV.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final CategoryRepository categoryRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final LikeRepository likeRepository;
    private final VoteService voteService;
    private final ChatRoomRepository chatRoomRepository;

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
                VoteOption option = VoteOption.builder().content(voteContent).build();
                post.addVoteOption(option);
            }
        }
        return postRepository.save(post);
    }

    @Transactional
    public void joinParty(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (post.getType() != PostType.PARTY) throw new IllegalStateException("팟모집 게시글이 아닙니다.");
        if (post.getCurrentParticipants() >= post.getMaxParticipants()) throw new IllegalStateException("인원이 가득 찼습니다.");
        post.setCurrentParticipants(post.getCurrentParticipants() + 1);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts() {
        return postRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPostDetail(Long postId, String email) {
        Post post = postRepository.findById(postId).orElseThrow();
        Member member = memberRepository.findByEmail(email).orElseThrow();

        boolean isVoted = voteRecordRepository.existsByMemberMemberIdAndPostPostId(member.getMemberId(), postId);
        boolean isLiked = likeRepository.findByMemberAndPost(member, post).isPresent();

        List<VoteDto.VoteResponse> voteOptions = voteService.getVoteDetails(email, postId).stream()
                .map(option -> VoteDto.VoteResponse.builder().optionId(option.getId()).content(option.getContent()).count(option.getCount()).build())
                .collect(Collectors.toList());

        List<PostResponseDto.CommentResponseDto> comments = post.getComments().stream()
                .map(comment -> PostResponseDto.CommentResponseDto.builder()
                        .commentId(comment.getCommentId())
                        .content(comment.getContent())
                        .authorNickname(comment.getMember() != null ? comment.getMember().getNickname() : "알 수 없음")
                        .createdAt(comment.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PostResponseDto.builder()
                .postId(post.getPostId()).title(post.getTitle()).content(post.getContent()).type(post.getType())
                .authorNickname(post.getMember().getNickname()).createdAt(post.getCreatedAt())
                .isVoted(isVoted).isLiked(isLiked).likeCount(post.getLikes().size())
                .voteOptions(voteOptions).currentParticipants(post.getCurrentParticipants())
                .maxParticipants(post.getMaxParticipants()).comments(comments)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getMyPosts(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        return postRepository.findByMember(member).stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getPostsICommented(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        return postRepository.findDistinctPostsByComments_Member(member).stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private PostResponseDto convertToDto(Post post) {
        return PostResponseDto.builder()
                .postId(post.getPostId()).title(post.getTitle()).content(post.getContent()).type(post.getType())
                .authorNickname(post.getMember().getNickname()).createdAt(post.getCreatedAt())
                .currentParticipants(post.getCurrentParticipants()).maxParticipants(post.getMaxParticipants())
                .likeCount(post.getLikes() != null ? post.getLikes().size() : 0).build();
    }

    @Transactional
    public void updatePost(Long postId, PostRequestDto dto, String email) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getMember().getEmail().equals(email)) throw new RuntimeException("권한이 없습니다.");
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());

        chatRoomRepository.findByPostId(postId).ifPresent(chatRoom -> {
            chatRoom.setRoomName(dto.getTitle());
        });
    }

    @Transactional
    public void deletePost(Long postId, String email) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getMember().getEmail().equals(email)) throw new RuntimeException("권한이 없습니다.");
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getPostsByRoom(Long roomId) {
        return postRepository.findByRoom_RoomId(roomId).stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getPostsByCategory(Long categoryId) {
        return postRepository.findByCategory_CategoryId(categoryId).stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRoomDashboardData(Long roomId) {
        Map<String, Object> dashboard = new HashMap<>();
        List<Post> roomPosts = postRepository.findByRoom_RoomId(roomId);
        dashboard.put("attendance", roomPosts.stream().filter(p -> p.getType() == PostType.ATTENDANCE).sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt())).findFirst().map(this::convertToDto).orElse(null));
        dashboard.put("presenter", roomPosts.stream().filter(p -> p.getType() == PostType.PRESENTER).findFirst().map(this::convertToDto).orElse(null));
        dashboard.put("schedules", roomPosts.stream().filter(p -> p.getType() == PostType.SCHEDULE).map(this::convertToDto).collect(Collectors.toList()));
        return dashboard;
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getHot3Posts() {
        return postRepository.findAll().stream()
                .sorted((p1, p2) -> Integer.compare(p2.getLikes() != null ? p2.getLikes().size() : 0, p1.getLikes() != null ? p1.getLikes().size() : 0))
                .limit(3).map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public void toggleLike(Long postId, String email) {
        Post post = postRepository.findById(postId).orElseThrow();
        Member member = memberRepository.findByEmail(email).orElseThrow();
        Optional<Like> existingLike = likeRepository.findByMemberAndPost(member, post);
        if (existingLike.isPresent()) likeRepository.delete(existingLike.get());
        else likeRepository.save(Like.builder().member(member).post(post).build());
        if (likeRepository.countByPost(post) > 5) post.setHot(true);
    }
}