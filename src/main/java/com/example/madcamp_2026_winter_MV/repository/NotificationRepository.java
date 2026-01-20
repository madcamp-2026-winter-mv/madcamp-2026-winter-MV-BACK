package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Member;
import com.example.madcamp_2026_winter_MV.entity.Notification;
import com.example.madcamp_2026_winter_MV.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByReceiverOrderByCreatedAtDesc(Member receiver);

    long countByReceiverAndIsReadFalse(Member receiver);

    /** 사이드바용: COMMENT, CHAT_INVITE 중 읽지 않은 개수 (알람3=채팅 미읽음은 제외) */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.receiver = :receiver AND n.isRead = false AND n.type IN :types")
    long countByReceiverAndIsReadFalseAndTypeIn(@Param("receiver") Member receiver, @Param("types") List<NotificationType> types);
}