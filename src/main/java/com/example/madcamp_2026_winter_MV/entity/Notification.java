package com.example.madcamp_2026_winter_MV.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id") // 요청하신 대로 PK 이름을 명시적으로 설정
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver; // 알림을 받는 사용자 (게시글 작성자)

    @Column(nullable = false)
    private String content; // 알림 내용

    @Column(nullable = false)
    private String url; // 클릭 시 이동할 페이지 주소

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false; // 읽음 여부

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 알림 발생 시간

    // 알림 읽음 처리 메서드
    public void markAsRead() {
        this.isRead = true;
    }
}