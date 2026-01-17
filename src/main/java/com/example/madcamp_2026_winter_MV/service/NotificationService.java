package com.example.madcamp_2026_winter_MV.service;

import com.example.madcamp_2026_winter_MV.dto.NotificationResponseDto;
import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Notification;
import com.example.madcamp_2026_winter_MV.entity.Post;
import com.example.madcamp_2026_winter_MV.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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

    // 1. SSE 연결 설정
    public SseEmitter subscribe(String email) {
        SseEmitter emitter = new SseEmitter(60L * 1000 * 60);
        emitters.put(email, emitter);

        // 연결 종료/타임아웃 시 삭제
        emitter.onCompletion(() -> emitters.remove(email));
        emitter.onTimeout(() -> emitters.remove(email));

        // 첫 연결 시 503 에러 방지용 더미 데이터 전송
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            emitters.remove(email);
        }
        return emitter;
    }

    // 2. 알림 생성 및 실시간 전송
    @Transactional
    public void createNotification(Member receiver, Post post, String content) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .content(content)
                .url("/posts/" + post.getPostId())
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        // 실시간 전송 (SSE)
        String email = receiver.getEmail();
        if (emitters.containsKey(email)) {
            SseEmitter emitter = emitters.get(email);
            try {
                // 알림 데이터를 DTO로 변환하여 전송
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(NotificationResponseDto.from(notification)));
            } catch (IOException e) {
                emitters.remove(email);
            }
        }
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
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}