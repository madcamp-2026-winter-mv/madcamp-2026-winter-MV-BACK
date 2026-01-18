package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.ChatMessageDto;
import com.example.madcamp_2026_winter_MV.dto.ChatRoomResponseDto;
import com.example.madcamp_2026_winter_MV.entity.*;
import com.example.madcamp_2026_winter_MV.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;

    //  1. 팟 확정 및 채팅방 생성 (수정: ChatMember 저장 로직 추가)
    @Transactional
    public Long confirmAndCreateChat(Long postId, List<Long> selectedMemberIds, String ownerEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        Member owner = memberRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (post.getType() != PostType.PARTY) {
            throw new IllegalStateException("팟 모집 전용 게시글이 아닙니다.");
        }

        if (!post.getMember().getMemberId().equals(owner.getMemberId())) {
            throw new IllegalStateException("작성자만 팟을 확정할 수 있습니다.");
        }

        int totalPartyMembers = selectedMemberIds.size() + 1;
        if (totalPartyMembers > post.getMaxParticipants()) {
            throw new IllegalArgumentException("최대 모집 인원을 초과할 수 없습니다.");
        }

        post.setClosed(true);
        post.setCurrentParticipants(totalPartyMembers);

        ChatRoom chatRoom = ChatRoom.builder()
                .roomName(post.getTitle())
                .postId(postId)
                .createdAt(LocalDateTime.now())
                .build();
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        // 작성자와 선택된 멤버들을 채팅방 참여자로 등록
        saveChatMember(savedRoom, owner); // 방장 등록
        for (Long mId : selectedMemberIds) {
            Member m = memberRepository.findById(mId).orElseThrow();
            saveChatMember(savedRoom, m); // 멤버 등록
        }

        // 시스템 환영 메시지 자동 저장
        ChatMessage welcomeMessage = ChatMessage.builder()
                .chatRoom(savedRoom)
                .senderNickname("몰입캠프 ")
                .content("'" + post.getTitle() + "'에서 채팅방이 시작되었습니다. 대화를 시작해보세요!")
                .timestamp(LocalDateTime.now())
                .build();
        chatMessageRepository.save(welcomeMessage);

        return savedRoom.getChatRoomId();
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


    // 4. 팟 멤버 추가 (비었을 때 추가 영입)
    @Transactional
    public void addMemberToParty(Long chatRoomId, Long newMemberId, String ownerEmail) {
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
    }

    //  5. 팟 멤버 내보내기
    @Transactional
    public void leaveParty(Long chatRoomId, Long targetMemberId, String requestUserEmail) {
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

        // ChatMemberRepository에 정의한 findByMember(member)를 호출합니다.
        return chatMemberRepository.findByMember(member).stream()
                .map(cm -> {
                    ChatRoom room = cm.getChatRoom();
                    return ChatRoomResponseDto.builder()
                            .chatRoomId(room.getChatRoomId())
                            .roomName(room.getRoomName())
                            .postId(room.getPostId())
                            .createdAt(room.getCreatedAt().toString())
                            .build();
                })
                .collect(Collectors.toList());
    }
}