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

    // 알림 서비스 추가
    private final NotificationService notificationService;

    // 1. 팟 확정 및 채팅방 생성
    @Transactional
    public Long confirmAndCreateChat(Long postId, List<Long> selectedMemberIds, String ownerEmail) {
        log.info("[채팅방개설] Service 시작 postId={} selectedMemberIds={} ownerEmail={}",
                postId, selectedMemberIds, (ownerEmail != null && ownerEmail.length() > 2) ? ownerEmail.substring(0, 2) + "***" : ownerEmail);

        try {
            if (chatRoomRepository.existsByPostId(postId)) {
                log.warn("[채팅방개설] 이미 채팅방이 존재함 postId={}", postId);
                throw new IllegalStateException("이미 채팅방이 개설된 게시글입니다.");
            }

            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

            Member owner = memberRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            if (post.getType() != PostType.PARTY) {
                throw new IllegalStateException("팟 모집 전용 게시글이 아닙니다.");
            }

            if (post.getMember() == null || !post.getMember().getMemberId().equals(owner.getMemberId())) {
                throw new IllegalStateException("작성자만 팟을 확정할 수 있습니다.");
            }

            int max = (post.getMaxParticipants() != null) ? post.getMaxParticipants() : 0;
            int totalPartyMembers = (selectedMemberIds != null ? selectedMemberIds.size() : 0) + 1;
            if (max > 0 && totalPartyMembers > max) {
                throw new IllegalArgumentException("최대 모집 인원을 초과할 수 없습니다.");
            }

            post.setClosed(true);
            post.setCurrentParticipants(totalPartyMembers);

            ChatRoom chatRoom = ChatRoom.builder()
                    .roomName(post.getTitle())
                    .postId(postId)
                    .createdAt(LocalDateTime.now())
                    .build();

            ChatRoom savedRoom = chatRoomRepository.saveAndFlush(chatRoom);
            log.info("[채팅방개설] ChatRoom 저장 완료 ID={}", savedRoom.getChatRoomId());

            // 방장 등록
            saveChatMember(savedRoom, owner);

            // 참여자 등록 및 알림 발송
            if (selectedMemberIds != null) {
                for (Long mId : selectedMemberIds) {
                    Member m = memberRepository.findById(mId)
                            .orElseThrow(() -> new IllegalArgumentException("참가자 ID " + mId + "를 찾을 수 없습니다."));
                    saveChatMember(savedRoom, m);

                    // [추가] 팟 확정 알림 발송 (CHAT_INVITE)
                    if (m.isAllowAlarm()) {
                        notificationService.createNotificationForChatInvite(
                                m,
                                "'" + post.getTitle() + "' 팟의 최종 멤버로 확정되었습니다. 채팅방에 참여해보세요!",
                                "/chat?room=" + savedRoom.getChatRoomId()
                        );
                    }
                }
            }

            ChatMessage welcomeMessage = ChatMessage.builder()
                    .chatRoom(savedRoom)
                    .senderNickname("몰입캠프")
                    .content("'" + post.getTitle() + "'게시글의 팟이 확정되었습니다!")
                    .timestamp(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(welcomeMessage);

            postTempParticipantRepository.deleteByPost_PostId(postId);
            log.info("[채팅방개설] 임시참가자 삭제 완료");

            return savedRoom.getChatRoomId();

        } catch (Exception ex) {
            log.error("[채팅방개설] 실패 - 원인: {}", ex.getMessage());
            throw ex;
        }
    }

    /** 팟 작성자가 댓글 작성자를 임시 참가자로 토글. */
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
            if (current + 1 + 1 > max) {
                throw new IllegalStateException("최대 모집 인원을 초과할 수 없습니다.");
            }
            Member toAdd = memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
            postTempParticipantRepository.save(PostTempParticipant.builder().post(post).member(toAdd).build());
        }
        return postTempParticipantRepository.findByPost_PostId(postId).stream()
                .map(pp -> pp.getMember().getMemberId())
                .collect(Collectors.toList());
    }

    // 2. 실시간 메시지 DB 저장
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

    // 3. 과거 채팅 내역 불러오기 (발신자 프로필 이미지: 채팅방 멤버 기준 닉네임 매칭)
    public List<ChatMessageDto> getChatMessages(Long chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        java.util.Map<String, String> nicknameToImage = chatMemberRepository.findByChatRoom(room).stream()
                .filter(cm -> cm.getMember() != null)
                .collect(Collectors.toMap(cm -> cm.getMember().getNickname(), cm -> cm.getMember().getProfileImage() != null ? cm.getMember().getProfileImage() : "", (a, b) -> a));

        return chatMessageRepository.findByChatRoom_ChatRoomIdOrderByTimestampAsc(chatRoomId)
                .stream()
                .map(m -> {
                    String img = nicknameToImage.get(m.getSenderNickname());
                    return ChatMessageDto.builder()
                            .chatRoomId(chatRoomId)
                            .senderNickname(m.getSenderNickname())
                            .senderProfileImageUrl((img != null && !img.isEmpty()) ? img : null)
                            .content(m.getContent())
                            .timestamp(m.getTimestamp().toString())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 4. 팟 멤버 추가
    @Transactional
    public String addMemberToParty(Long chatRoomId, Long newMemberId, String ownerEmail) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId).orElseThrow();
        Post post = postRepository.findById(room.getPostId()).orElseThrow();
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();

        if (!post.getMember().getMemberId().equals(owner.getMemberId())) {
            throw new IllegalStateException("작성자만 멤버를 추가할 수 있습니다.");
        }

        long currentCount = chatMemberRepository.countByChatRoom(room);
        if (currentCount >= post.getMaxParticipants()) {
            throw new IllegalStateException("이미 꽉 찬 팟입니다.");
        }

        Member newMember = memberRepository.findById(newMemberId).orElseThrow();
        saveChatMember(room, newMember);

        post.setCurrentParticipants((int) chatMemberRepository.countByChatRoom(room));

        // 채팅방 초대 알림 (CHAT_INVITE)
        if (newMember.isAllowAlarm()) {
            notificationService.createNotificationForChatInvite(
                    newMember,
                    "'" + post.getTitle() + "' 채팅방에 초대되었습니다.",
                    "/chat?room=" + chatRoomId
            );
        }

        return newMember.getNickname();
    }

    // 5. 팟 멤버 내보내기
    @Transactional
    public String leaveParty(Long chatRoomId, Long targetMemberId, String requestUserEmail) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId).orElseThrow();
        Member target = memberRepository.findById(targetMemberId).orElseThrow();
        Member requestUser = memberRepository.findByEmail(requestUserEmail).orElseThrow();
        Post post = postRepository.findById(room.getPostId()).orElseThrow();

        boolean isOwner = post.getMember().getMemberId().equals(requestUser.getMemberId());
        boolean isSelf = target.getMemberId().equals(requestUser.getMemberId());

        if (!isOwner && !isSelf) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        chatMemberRepository.deleteByChatRoomAndMember(room, target);

        post.setCurrentParticipants((int) chatMemberRepository.countByChatRoom(room));
        if (post.getCurrentParticipants() < post.getMaxParticipants()) {
            post.setClosed(false);
        }

        return target.getNickname();
    }

    private void saveChatMember(ChatRoom room, Member member) {
        if (!chatMemberRepository.existsByChatRoomAndMember(room, member)) {
            chatMemberRepository.save(ChatMember.builder().chatRoom(room).member(member).build());
        }
    }

    /** 실시간 메시지 브로드캐스트 전 발신자 프로필 이미지 조회 (닉네임으로 채팅방 멤버 매칭) */
    public String findSenderProfileImageUrl(Long chatRoomId, String senderNickname) {
        if (chatRoomId == null || senderNickname == null) return null;
        ChatRoom room = chatRoomRepository.findById(chatRoomId).orElse(null);
        if (room == null) return null;
        return chatMemberRepository.findByChatRoom(room).stream()
                .filter(cm -> cm.getMember() != null && senderNickname.equals(cm.getMember().getNickname()))
                .map(cm -> cm.getMember().getProfileImage())
                .filter(img -> img != null && !img.isEmpty())
                .findFirst()
                .orElse(null);
    }

    // 6. 채팅방 읽음 시간 업데이트
    @Transactional
    public void updateLastReadTime(Long chatRoomId, String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        ChatRoom room = chatRoomRepository.findById(chatRoomId).orElseThrow();
        ChatMember chatMember = chatMemberRepository.findByChatRoomAndMember(room, member)
                .orElseThrow(() -> new IllegalArgumentException("참여 중인 채팅방이 아닙니다."));
        chatMember.setLastReadAt(LocalDateTime.now());
    }

    // 7. 내가 참여 중인 모든 채팅방 목록 조회 (미읽음 카운트 로직 추가)
    public List<ChatRoomResponseDto> getMyChatRooms(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return chatMemberRepository.findByMemberWithChatRoom(member).stream()
                .map(cm -> {
                    ChatRoom room = cm.getChatRoom();
                    String postTitle = postRepository.findById(room.getPostId())
                            .map(Post::getTitle)
                            .orElse("게시글 #" + room.getPostId());

                    int participantCount = (int) chatMemberRepository.countByChatRoom(room);

                    //  미읽음 메시지 수 계산
                    LocalDateTime lastRead = (cm.getLastReadAt() != null) ? cm.getLastReadAt() : room.getCreatedAt();
                    long unreadCount = chatMessageRepository.countUnreadMessages(room.getChatRoomId(), lastRead);

                    return ChatRoomResponseDto.builder()
                            .chatRoomId(room.getChatRoomId())
                            .roomName(room.getRoomName())
                            .postId(room.getPostId())
                            .postTitle(postTitle)
                            .createdAt(room.getCreatedAt().toString())
                            .participantCount(participantCount)
                            .unreadCount((int) unreadCount) // DTO에 필드 추가 필요
                            .build();
                })
                .collect(Collectors.toList());
    }
    // 8. 채팅방 삭제 (방장 전용) - 게시글 상태 변경 없음
    @Transactional
    public void deleteChatRoom(Long chatRoomId, String email) {
        // 1. 채팅방 조회
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 2. 연관된 게시글 조회 (권한 확인용)
        Post post = postRepository.findById(room.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("연관된 게시글을 찾을 수 없습니다."));

        // 3. 사용자 확인
        Member requester = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 4. 방장 권한 체크
        if (!post.getMember().getMemberId().equals(requester.getMemberId())) {
            throw new IllegalStateException("방장만 채팅방을 삭제할 수 있습니다.");
        }

        // 5. 채팅방 삭제 (연관된 ChatMember, ChatMessage는 Cascade에 의해 자동 삭제)
        chatRoomRepository.delete(room);

        log.info("[채팅방삭제] 방장({})이 채팅방(ID: {})을 삭제했습니다. 게시글 상태는 유지됩니다.", email, chatRoomId);
    }
}