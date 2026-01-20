package com.example.madcamp_2026_winter_MV.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomId;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "post_id", unique = true)
    private Long postId;

    /** 게시글 삭제 후에도 방장 판별용. 채팅방 생성 시 글쓴이(방장) memberId 저장 */
    @Column(name = "owner_member_id")
    private Long ownerMemberId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMember> chatMembers = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();
}