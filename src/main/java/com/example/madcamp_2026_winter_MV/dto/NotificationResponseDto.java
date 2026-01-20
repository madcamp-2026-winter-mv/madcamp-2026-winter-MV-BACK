package com.example.madcamp_2026_winter_MV.dto;

import com.example.madcamp_2026_winter_MV.entity.Notification;
import com.example.madcamp_2026_winter_MV.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDto {
    private Long notificationId;
    private String content;
    private String url;
    private String type; // "COMMENT" | "CHAT_INVITE" (기존 데이터는 null → "COMMENT"로 매핑)
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        String typeVal = (notification.getType() != null)
                ? notification.getType().name()
                : "COMMENT";
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .content(notification.getContent())
                .url(notification.getUrl())
                .type(typeVal)
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}