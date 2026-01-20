package com.example.madcamp_2026_winter_MV.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomId;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "post_id", unique = true)
    private Long postId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}