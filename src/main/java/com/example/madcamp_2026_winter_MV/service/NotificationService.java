package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.NotificationResponseDto;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Notification;
import com.example.madcamp_2026_winter_MV.entity.NotificationType;
import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** COMMENT: 댓글 알림. url = /community/{postId} */
    @Transactional
    public void createNotification(Member receiver, Post post, String content) {
        String url = "/community/" + post.getPostId();
        Notification notification = Notification.builder()
                .receiver(receiver)
                .content(content)
                .url(url)
                .type(NotificationType.COMMENT)
                .isRead(false)
                .build();
        saveAndEmit(receiver, notification);
    }

    /** CHAT_INVITE: 채팅방 초대 알림. url = /chat?room={chatRoomId} */
    @Transactional
    public void createNotificationForChatInvite(Member receiver, String content, String url) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .content(content)
                .url(url)
                .type(NotificationType.CHAT_INVITE)
                .isRead(false)
                .build();
        saveAndEmit(receiver, notification);
    }

    private void saveAndEmit(Member receiver, Notification notification) {
        notificationRepository.save(notification);
        String email = receiver.getEmail();
        if (emitters.containsKey(email)) {
            SseEmitter emitter = emitters.get(email);
            try {
                long unreadCount = notificationRepository.countByReceiverAndIsReadFalse(receiver);
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(Map.of(
                                "notification", NotificationResponseDto.from(notification),
                                "unreadCount", unreadCount
                        )));
            } catch (IOException e) {
                emitters.remove(email);
            }
        }
    }

    public SseEmitter subscribe(String email) {
        SseEmitter emitter = new SseEmitter(60L * 1000 * 60);
        emitters.put(email, emitter);
        emitter.onCompletion(() -> emitters.remove(email));
        emitter.onTimeout(() -> emitters.remove(email));
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            emitters.remove(email);
        }
        return emitter;
    }

    public long getUnreadNotificationCount(Member member) {
        return notificationRepository.countByReceiverAndIsReadFalse(member);
    }

    /** 사이드바용: COMMENT, CHAT_INVITE만 (알람3 채팅 미읽음 제외) */
    public long getUnreadCountForSidebar(Member member) {
        return notificationRepository.countByReceiverAndIsReadFalseAndTypeIn(
                member, Arrays.asList(NotificationType.COMMENT, NotificationType.CHAT_INVITE));
    }

    public List<NotificationResponseDto> getNotifications(Member member) {
        return notificationRepository.findAllByReceiverOrderByCreatedAtDesc(member)
                .stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public String clickNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow();
        notification.markAsRead();
        return notification.getUrl();
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow();
        notification.markAsRead();
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}