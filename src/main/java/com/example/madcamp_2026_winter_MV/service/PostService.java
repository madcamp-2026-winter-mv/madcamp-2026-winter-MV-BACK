package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.PostRequestDto;
import com.example.madcamp_2026_winter_MV.dto.PostResponseDto;
import com.example.madcamp_2026_winter_MV.dto.VoteDto;
import com.example.madcamp_2026_winter_MV.entity.*;
import com.example.madcamp_2026_winter_MV.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
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

    // [추가] 몰입캠프 참여자(1-4분반) 검증 로직
    private void validateCampParticipant(Member member) {
        if (member.getRoom() == null) {
            throw new IllegalStateException("몰입캠프 참여자만 접근 가능합니다.");
        }
        Long roomId = member.getRoom().getRoomId();
        // 분반 ID가 1, 2, 3, 4 중 하나여야 함
        if (roomId < 1 || roomId > 4) {
            throw new IllegalStateException("몰입캠프 참여자만 접근 가능합니다.");
        }
    }

    @Transactional
    public Post createPost(PostRequestDto dto, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // [추가] 글쓰기 권한 체크
        validateCampParticipant(member);

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

        // [추가] 상세 보기 권한 체크
        validateCampParticipant(member);

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
        // [변경] 목록 노출 시 내용 요약 (제목 + 내용 조금)
        String contentSummary = post.getContent();
        if (contentSummary != null && contentSummary.length() > 50) {
            contentSummary = contentSummary.substring(0, 50) + "...";
        }

        return PostResponseDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(contentSummary)
                .type(post.getType())
                .authorNickname(post.getMember().getNickname())
                .createdAt(post.getCreatedAt())
                .currentParticipants(post.getCurrentParticipants())
                .maxParticipants(post.getMaxParticipants())
                .likeCount(post.getLikes() != null ? post.getLikes().size() : 0)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> searchPosts(String keyword) {
        return postRepository.findByTitleContainingOrContentContaining(keyword, keyword)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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

    // 1. 탭(카테고리)에 따른 게시글 목록 조회
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPostsForTab(String categoryName, Pageable pageable) {
        Page<Post> posts;

        if (categoryName == null || categoryName.equals("전체")) {
            posts = postRepository.findAll(pageable);
        } else if (categoryName.equals("팟모집")) {
            posts = postRepository.findByType(PostType.PARTY, pageable);
        } else {
            posts = postRepository.findByCategory_Name(categoryName, pageable);
        }

        // 프론트엔드 전용 DTO 변환 메서드 사용
        return posts.map(this::convertToFrontendDto);
    }

    // 2. 프론트엔드 포맷(author 객체, partyInfo 객체 등)에 맞춘 변환기
    private PostResponseDto convertToFrontendDto(Post post) {
        String timeAgo = formatTimeAgo(post.getCreatedAt());

        // 작성자 객체 생성
        String nickname = post.isAnonymous() ? "익명" : post.getMember().getNickname();
        PostResponseDto.AuthorDto authorDto = PostResponseDto.AuthorDto.builder()
                .nickname(nickname)
                .isAnonymous(post.isAnonymous())
                .imageUrl(null)
                .build();

        // 팟모집 정보 객체 생성 (타입이 PARTY일 때만)
        PostResponseDto.PartyInfoDto partyInfoDto = null;
        if (post.getType() == PostType.PARTY) {
            partyInfoDto = PostResponseDto.PartyInfoDto.builder()
                    .currentCount(post.getCurrentParticipants())
                    .maxCount(post.getMaxParticipants())
                    .isRecruiting(!post.isClosed() && post.getCurrentParticipants() < post.getMaxParticipants())
                    .build();
        }

        // 내용 요약
        String contentSummary = post.getContent();
        if (contentSummary != null && contentSummary.length() > 80) {
            contentSummary = contentSummary.substring(0, 80) + "...";
        }

        return PostResponseDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(contentSummary)
                .type(post.getType())
                .authorNickname(nickname)
                .createdAt(post.getCreatedAt())
                .currentParticipants(post.getCurrentParticipants())
                .maxParticipants(post.getMaxParticipants())
                .likeCount(post.getLikes().size())
                .categoryName(post.getCategory().getName())
                .timeAgo(timeAgo)
                .author(authorDto)
                .partyInfo(partyInfoDto)
                .build();
    }

    // 3. 시간 계산 유틸
    private String formatTimeAgo(LocalDateTime time) {
        if (time == null) return "";
        Duration duration = Duration.between(time, LocalDateTime.now());
        long min = duration.toMinutes();

        if (min < 1) return "방금 전";
        if (min < 60) return min + "분 전";

        long hour = duration.toHours();
        if (hour < 24) return hour + "시간 전";

        return time.toLocalDate().toString();
    }
}