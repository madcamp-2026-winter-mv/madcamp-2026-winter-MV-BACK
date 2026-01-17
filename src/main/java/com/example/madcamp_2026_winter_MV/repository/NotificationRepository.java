package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 특정 사용자의 알림 목록을 최신순으로 조회
    List<Notification> findAllByReceiverOrderByCreatedAtDesc(Member receiver);

    // 읽지 않은 알림 개수 카운트
    long countByReceiverAndIsReadFalse(Member receiver);
}