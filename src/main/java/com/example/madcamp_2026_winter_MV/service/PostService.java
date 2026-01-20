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
    private final ChatMemberRepository chatMemberRepository;
    private final PostTempParticipantRepository postTempParticipantRepository;

    //  몰입캠프 참여자(1-4분반) 검증 로직
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

        // 글쓰기 권한 체크
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
                .isAnonymous(Boolean.TRUE.equals(dto.getIsAnonymous()))
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

        // 상세 보기 권한 체크
        validateCampParticipant(member);

        boolean isVoted = voteRecordRepository.existsByMemberMemberIdAndPostPostId(member.getMemberId(), postId);
        boolean isLiked = likeRepository.findByMemberAndPost(member, post).isPresent();

        List<VoteOption> voteOpts = voteService.getVoteDetails(email, postId);
        int totalVotes = voteOpts.stream().mapToInt(VoteOption::getCount).sum();
        List<VoteDto.VoteResponse> voteOptions = voteOpts.stream()
                .map(opt -> VoteDto.VoteResponse.builder()
                        .optionId(opt.getId())
                        .content(opt.getContent())
                        .count(opt.getCount())
                        .percentage(totalVotes == 0 ? 0.0 : (double) opt.getCount() / totalVotes * 100)
                        .build())
                .collect(Collectors.toList());

        // 댓글 목록 빌드. 익명이면 분반(roomId)·프로필 미노출. isMine은 이메일 기준(익명 댓글도 수정/삭제 가능).
        List<PostResponseDto.CommentResponseDto> comments = post.getComments().stream()
                .map(comment -> {
                    Long cRoomId = (comment.isAnonymous() || comment.getMember() == null || comment.getMember().getRoom() == null)
                            ? null : comment.getMember().getRoom().getRoomId();
                    String cImg = comment.isAnonymous() || comment.getMember() == null ? null : comment.getMember().getProfileImage();
                    boolean isMine = comment.getMember() != null && comment.getMember().getEmail().equals(email);
                    return PostResponseDto.CommentResponseDto.builder()
                            .commentId(comment.getCommentId())
                            .memberId(comment.getMember().getMemberId())
                            .content(comment.getContent())
                            .authorNickname(comment.isAnonymous() ? "익명" : (comment.getMember() != null ? comment.getMember().getNickname() : "알 수 없음"))
                            .createdAt(comment.getCreatedAt())
                            .isAnonymous(comment.isAnonymous())
                            .roomId(cRoomId)
                            .imageUrl(cImg)
                            .isMine(isMine)
                            .build();
                })
                .collect(Collectors.toList());

        // 익명이면 분반(roomId)·프로필 미노출. DTO에는 작성자(member)가 있으나 isAnonymous이면 외부에 익명 노출.
        Long authorRoomId = post.isAnonymous() ? null : (post.getMember() == null || post.getMember().getRoom() == null ? null : post.getMember().getRoom().getRoomId());
        String authorNickname = post.isAnonymous() ? "익명" : post.getMember().getNickname();
        String authorImg = post.isAnonymous() || post.getMember() == null ? null : post.getMember().getProfileImage();
        PostResponseDto.AuthorDto authorDto = PostResponseDto.AuthorDto.builder()
                .nickname(authorNickname)
                .isAnonymous(post.isAnonymous())
                .imageUrl(authorImg)
                .roomId(authorRoomId)
                .build();

        List<Long> tempParticipantIds = null;
        int displayCurrent = post.getCurrentParticipants();
        if (post.getType() == PostType.PARTY) {
            tempParticipantIds = postTempParticipantRepository.findByPost_PostId(postId).stream()
                    .map(pp -> pp.getMember().getMemberId())
                    .collect(Collectors.toList());
            displayCurrent = post.isClosed() ? post.getCurrentParticipants() : (1 + tempParticipantIds.size());
        }

        PostResponseDto.PartyInfoDto partyInfoDto = null;
        Long chatRoomId = null;
        boolean isChatParticipant = false;
        List<PostResponseDto.ChatParticipantDto> chatParticipants = List.of();
        if (post.getType() == PostType.PARTY) {
            int max = post.getMaxParticipants() != null ? post.getMaxParticipants() : 0;
            partyInfoDto = PostResponseDto.PartyInfoDto.builder()
                    .currentCount(displayCurrent)
                    .maxCount(max)
                    .isRecruiting(!post.isClosed() && displayCurrent < max)
                    .build();
            var chatRoomOpt = chatRoomRepository.findByPostId(postId);
            if (chatRoomOpt.isPresent()) {
                var room = chatRoomOpt.get();
                chatRoomId = room.getChatRoomId();
                isChatParticipant = chatMemberRepository.existsByChatRoomAndMember(room, member);
                Long authorMemberId = post.getMember() != null ? post.getMember().getMemberId() : null;
                chatParticipants = chatMemberRepository.findByChatRoom(room).stream()
                        .filter(cm -> cm.getMember() != null && (authorMemberId == null || !cm.getMember().getMemberId().equals(authorMemberId)))
                        .map(cm -> PostResponseDto.ChatParticipantDto.builder()
                                .nickname(cm.getMember().getNickname())
                                .imageUrl(cm.getMember().getProfileImage())
                                .build())
                        .collect(Collectors.toList());
            }
        }

        return PostResponseDto.builder()
                .postId(post.getPostId()).title(post.getTitle()).content(post.getContent()).type(post.getType())
                .authorNickname(authorNickname).createdAt(post.getCreatedAt())
                .isVoted(isVoted).isLiked(isLiked).likeCount(post.getLikes().size())
                .voteOptions(voteOptions).currentParticipants(displayCurrent)
                .maxParticipants(post.getMaxParticipants()).comments(comments)
                .isAuthor(post.getMember().getEmail().equals(email))
                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .timeAgo(formatTimeAgo(post.getCreatedAt()))
                .author(authorDto)
                .partyInfo(partyInfoDto)
                .tempParticipantIds(tempParticipantIds)
                .chatRoomId(chatRoomId)
                .isChatParticipant(isChatParticipant)
                .chatParticipants(chatParticipants)
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
        return convertToFrontendDto(post); // 로직 통합을 위해 convertToFrontendDto 호출
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> searchPosts(String keyword, Pageable pageable) {
        // Repository에서 페이징이 적용된 검색 결과를 가져옴
        return postRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable)
                .map(this::convertToFrontendDto);
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

        return posts.map(this::convertToFrontendDto);
    }

    // 2. 프론트엔드 포맷(author 객체, partyInfo 객체 등)에 맞춘 변환기
    private PostResponseDto convertToFrontendDto(Post post) {
        String timeAgo = formatTimeAgo(post.getCreatedAt());

        // 작성자 객체 생성. 익명이면 분반·프로필 미노출.
        String nickname = post.isAnonymous() ? "익명" : post.getMember().getNickname();
        Long aRoomId = post.isAnonymous() ? null : (post.getMember() == null || post.getMember().getRoom() == null ? null : post.getMember().getRoom().getRoomId());
        String aImg = post.isAnonymous() || post.getMember() == null ? null : post.getMember().getProfileImage();
        PostResponseDto.AuthorDto authorDto = PostResponseDto.AuthorDto.builder()
                .nickname(nickname)
                .isAnonymous(post.isAnonymous())
                .imageUrl(aImg)
                .roomId(aRoomId)
                .build();

        // 팟모집: 모집중이면 currentCount = 1 + 임시 참가자 수
        PostResponseDto.PartyInfoDto partyInfoDto = null;
        int displayCurrent = post.getCurrentParticipants();
        if (post.getType() == PostType.PARTY) {
            int tempSize = postTempParticipantRepository.findByPost_PostId(post.getPostId()).size();
            displayCurrent = post.isClosed() ? post.getCurrentParticipants() : (1 + tempSize);
            int max = post.getMaxParticipants() != null ? post.getMaxParticipants() : 0;
            partyInfoDto = PostResponseDto.PartyInfoDto.builder()
                    .currentCount(displayCurrent)
                    .maxCount(max)
                    .isRecruiting(!post.isClosed() && displayCurrent < max)
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
                .currentParticipants(displayCurrent)
                .maxParticipants(post.getMaxParticipants())
                .likeCount(post.getLikes() != null ? post.getLikes().size() : 0)
                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
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