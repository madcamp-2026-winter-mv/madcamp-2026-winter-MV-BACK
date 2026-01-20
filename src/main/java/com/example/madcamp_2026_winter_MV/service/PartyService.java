package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.ChatMessageDto;
import com.example.madcamp_2026_winter_MV.dto.ChatRoomResponseDto;
import com.example.madcamp_2026_winter_MV.entity.*;
import com.example.madcamp_2026_winter_MV.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService {

    private static final Logger log = LoggerFactory.getLogger(PartyService.class);
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final PostTempParticipantRepository postTempParticipantRepository;
    private final CommentRepository commentRepository;

    //  1. 팟 확정 및 채팅방 생성 (수정: ChatMember 저장 로직 추가)
    @Transactional
    public Long confirmAndCreateChat(Long postId, List<Long> selectedMemberIds, String ownerEmail) {
        log.info("[채팅방개설] Service 시작 postId={} selectedMemberIds={} ownerEmail={}",
                postId, selectedMemberIds, (ownerEmail != null && ownerEmail.length() > 2) ? ownerEmail.substring(0, 2) + "***" : ownerEmail);
        try {
            if (selectedMemberIds == null) {
                log.warn("[채팅방개설] selectedMemberIds==null");
            }
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
            log.info("[채팅방개설] Post 조회 OK postId={} type={} maxParticipants={} getMember==null?{}",
                    postId, post.getType(), post.getMaxParticipants(), (post.getMember() == null));

            Member owner = memberRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            log.info("[채팅방개설] Owner 조회 OK ownerId={}", owner.getMemberId());

            if (post.getType() != PostType.PARTY) {
                log.warn("[채팅방개설] 검증 실패: type != PARTY");
                throw new IllegalStateException("팟 모집 전용 게시글이 아닙니다.");
            }

            if (post.getMember() == null || !post.getMember().getMemberId().equals(owner.getMemberId())) {
                log.warn("[채팅방개설] 검증 실패: 작성자 불일치");
                throw new IllegalStateException("작성자만 팟을 확정할 수 있습니다.");
            }

            int max = (post.getMaxParticipants() != null) ? post.getMaxParticipants() : 0;
            int totalPartyMembers = (selectedMemberIds != null ? selectedMemberIds.size() : 0) + 1;
            log.info("[채팅방개설] 인원 검증 max={} totalPartyMembers={}", max, totalPartyMembers);
            if (max > 0 && totalPartyMembers > max) {
                log.warn("[채팅방개설] 검증 실패: 인원 초과");
                throw new IllegalArgumentException("최대 모집 인원을 초과할 수 없습니다.");
            }

            post.setClosed(true);
            post.setCurrentParticipants(totalPartyMembers);
            log.info("[채팅방개설] Post 상태 갱신 setClosed=true currentParticipants={}", totalPartyMembers);

            ChatRoom chatRoom = ChatRoom.builder()
                    .roomName(post.getTitle())
                    .postId(postId)
                    .createdAt(LocalDateTime.now())
                    .build();
            ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
            log.info("[채팅방개설] ChatRoom 저장 OK chatRoomId={} roomName={}", savedRoom.getChatRoomId(), savedRoom.getRoomName());

            saveChatMember(savedRoom, owner); // 방장 등록
            log.info("[채팅방개설] 방장 등록 OK");

            List<Long> ids = (selectedMemberIds != null) ? selectedMemberIds : java.util.Collections.emptyList();
            for (Long mId : ids) {
                Member m = memberRepository.findById(mId)
                        .orElseThrow(() -> new IllegalArgumentException("참가자 ID " + mId + "를 찾을 수 없습니다."));
                saveChatMember(savedRoom, m);
                log.info("[채팅방개설] 멤버 등록 OK mId={}", mId);
            }

            ChatMessage welcomeMessage = ChatMessage.builder()
                    .chatRoom(savedRoom)
                    .senderNickname("몰입캠프 ")
                    .content("'" + post.getTitle() + "'에서 채팅방이 시작되었습니다. 대화를 시작해보세요!")
                    .timestamp(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(welcomeMessage);
            log.info("[채팅방개설] 환영 메시지 저장 OK");

            postTempParticipantRepository.deleteByPost_PostId(postId);
            log.info("[채팅방개설] 임시참가자 삭제 OK postId={}", postId);

            log.info("[채팅방개설] Service 완료 chatRoomId={}", savedRoom.getChatRoomId());
            return savedRoom.getChatRoomId();
        } catch (Exception ex) {
            log.error("[채팅방개설] Service 예외 postId={} selectedMemberIds={} ex={} message={}",
                    postId, selectedMemberIds, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /** 팟 작성자가 댓글 작성자를 임시 참가자로 토글. 중복 미허용. 댓글 작성자만 추가 가능. */
    @Transactional
    public java.util.List<Long> toggleTempParticipant(Long postId, Long memberId, String authorEmail) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        Member author = memberRepository.findByEmail(authorEmail).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (post.getType() != PostType.PARTY) {
            throw new IllegalStateException("팟 모집 게시글이 아닙니다.");
        }
        if (!post.getMember().getMemberId().equals(author.getMemberId())) {
            throw new IllegalStateException("작성자만 참가자를 선택할 수 있습니다.");
        }
        if (post.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("작성자는 이미 참가자입니다.");
        }
        if (!commentRepository.existsByPost_PostIdAndMember_MemberId(postId, memberId)) {
            throw new IllegalArgumentException("해당 글에 댓글을 단 사용자만 참가자로 선택할 수 있습니다.");
        }

        java.util.Optional<PostTempParticipant> existing = postTempParticipantRepository.findByPost_PostIdAndMember_MemberId(postId, memberId);
        if (existing.isPresent()) {
            postTempParticipantRepository.delete(existing.get());
        } else {
            int max = post.getMaxParticipants() != null ? post.getMaxParticipants() : 0;
            long current = postTempParticipantRepository.findByPost_PostId(postId).size();
            if (current + 1 + 1 > max) { // 작성자 1 + 현재 임시 + 추가 1
                throw new IllegalStateException("최대 모집 인원을 초과할 수 없습니다.");
            }
            Member toAdd = memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
            postTempParticipantRepository.save(PostTempParticipant.builder().post(post).member(toAdd).build());
        }
        return postTempParticipantRepository.findByPost_PostId(postId).stream()
                .map(pp -> pp.getMember().getMemberId())
                .collect(Collectors.toList());
    }

    //  2. 실시간 메시지 DB 저장
    @Transactional
    public void saveMessage(ChatMessageDto dto) {
        ChatRoom room = chatRoomRepository.findById(dto.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderNickname(dto.getSenderNickname())
                .content(dto.getContent())
                .timestamp(LocalDateTime.now())
                .build();

        chatMessageRepository.save(message);
    }

    // 3. 과거 채팅 내역 불러오기
    public List<ChatMessageDto> getChatMessages(Long chatRoomId) {
        return chatMessageRepository.findByChatRoom_ChatRoomIdOrderByTimestampAsc(chatRoomId)
                .stream()
                .map(m -> ChatMessageDto.builder()
                        .chatRoomId(chatRoomId)
                        .senderNickname(m.getSenderNickname())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp().toString())
                        .build())
                .collect(Collectors.toList());
    }


    // 4. 팟 멤버 추가
    @Transactional
    public String addMemberToParty(Long chatRoomId, Long newMemberId, String ownerEmail) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId).orElseThrow();
        Post post = postRepository.findById(room.getPostId()).orElseThrow();
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();

        // 방장 권한 체크
        if (!post.getMember().getMemberId().equals(owner.getMemberId())) {
            throw new IllegalStateException("작성자만 멤버를 추가할 수 있습니다.");
        }

        // 인원 초과 체크
        long currentCount = chatMemberRepository.countByChatRoom(room);
        if (currentCount >= post.getMaxParticipants()) {
            throw new IllegalStateException("이미 꽉 찬 팟입니다.");
        }

        Member newMember = memberRepository.findById(newMemberId).orElseThrow();
        saveChatMember(room, newMember);

        // 게시글의 현재 인원수 업데이트
        post.setCurrentParticipants((int) chatMemberRepository.countByChatRoom(room));

        return newMember.getNickname();
    }

    //  5. 팟 멤버 내보내기
    @Transactional
    public String leaveParty(Long chatRoomId, Long targetMemberId, String requestUserEmail) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId).orElseThrow();
        Member target = memberRepository.findById(targetMemberId).orElseThrow();
        Member requestUser = memberRepository.findByEmail(requestUserEmail).orElseThrow();
        Post post = postRepository.findById(room.getPostId()).orElseThrow();

        // 본인이 나가거나, 혹은 작성자가 강퇴하는 경우만 허용
        boolean isOwner = post.getMember().getMemberId().equals(requestUser.getMemberId());
        boolean isSelf = target.getMemberId().equals(requestUser.getMemberId());

        if (!isOwner && !isSelf) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        chatMemberRepository.deleteByChatRoomAndMember(room, target);

        // 인원이 빠졌으니 게시글 상태 업데이트 (다시 모집 중으로 변경 가능)
        post.setCurrentParticipants((int) chatMemberRepository.countByChatRoom(room));
        if (post.getCurrentParticipants() < post.getMaxParticipants()) {
            post.setClosed(false); // 빈자리가 생겼으므로 다시 모집 중 상태로
        }

        return target.getNickname();
    }

    private void saveChatMember(ChatRoom room, Member member) {
        if (!chatMemberRepository.existsByChatRoomAndMember(room, member)) {
            chatMemberRepository.save(ChatMember.builder().chatRoom(room).member(member).build());
        }
    }
    // 6. 내가 참여 중인 모든 채팅방 목록 조회
    public List<ChatRoomResponseDto> getMyChatRooms(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return chatMemberRepository.findByMember(member).stream()
                .map(cm -> {
                    ChatRoom room = cm.getChatRoom();
                    String postTitle = postRepository.findById(room.getPostId())
                            .map(Post::getTitle)
                            .orElse("게시글 #" + room.getPostId());
                    int participantCount = (int) chatMemberRepository.countByChatRoom(room);
                    return ChatRoomResponseDto.builder()
                            .chatRoomId(room.getChatRoomId())
                            .roomName(room.getRoomName())
                            .postId(room.getPostId())
                            .postTitle(postTitle)
                            .createdAt(room.getCreatedAt().toString())
                            .participantCount(participantCount)
                            .build();
                })
                .collect(Collectors.toList());
    }
}